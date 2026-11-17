package com.controllg.app

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.util.UUID

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "ajustes")

/** Una tele que maneja la app, con sus propios códigos y botones de apps. */
data class TvConfig(
    val id: String,
    val name: String,
    val profileId: String = PROFILE_STANDARD,
    /** Códigos cambiados en el editor, por perfil: BOTON → "0x…" o "" (sin código). */
    val overrides: Map<String, Map<String, String>> = emptyMap(),
    val hiddenApps: Set<RemoteKey> = DEFAULT_HIDDEN_APPS,
    /** Tipo de señal elegido por la Búsqueda automática (null = el del perfil). */
    val protocolOverride: IrProtocol? = null,
    /** Control encontrado por la Búsqueda automática, para mostrarlo. */
    val foundRemote: String? = null,
)

/** Teles de partida: la LG de la cocina y la AOC de la pieza de Pipi. */
val DEFAULT_TVS = listOf(
    TvConfig(id = "cocina", name = "LG cocina", profileId = PROFILE_STANDARD),
    TvConfig(id = "pieza", name = "Pieza Pipi", profileId = PROFILE_AOC),
)

data class AppSettings(
    val tvs: List<TvConfig> = DEFAULT_TVS,
    val selectedTvId: String = DEFAULT_TVS.first().id,
    val keepScreenOn: Boolean = false,
    val wizardDone: Boolean = false,
) {
    /** La tele que se está manejando ahora. */
    val tv: TvConfig get() = tvs.firstOrNull { it.id == selectedTvId } ?: tvs.first()

    val profileId: String get() = tv.profileId
    val overrides: Map<String, Map<String, String>> get() = tv.overrides
    val hiddenApps: Set<RemoteKey> get() = tv.hiddenApps
    val profile: CodeProfile get() = profileById(profileId)

    /** Tipo de señal con que se envían los códigos de la tele elegida. */
    val protocol: IrProtocol get() = tv.protocolOverride ?: profile.protocol

    /** Códigos que se envían de verdad: los de fábrica del perfil con los cambios del editor. */
    fun effectiveCodes(): Map<RemoteKey, Long?> {
        val changed = overrides[profile.id].orEmpty()
        val result = LinkedHashMap<RemoteKey, Long?>()
        for ((key, factory) in profile.codes) {
            val raw = changed[key.name]
            result[key] = when {
                raw == null -> factory
                raw.isEmpty() -> null
                else -> parseHexCode(raw) ?: factory
            }
        }
        return result
    }
}

class SettingsRepository(context: Context) {

    private val store = context.applicationContext.dataStore

    private object Keys {
        val TVS = stringPreferencesKey("teles")
        val SELECTED_TV = stringPreferencesKey("tele_actual")
        val KEEP_SCREEN_ON = booleanPreferencesKey("pantalla_encendida")
        val WIZARD_DONE = booleanPreferencesKey("asistente_listo")

        // Versión anterior (una sola tele): se migran a la LG de la cocina.
        val LEGACY_PROFILE = stringPreferencesKey("perfil")
        val LEGACY_OVERRIDES = stringPreferencesKey("codigos_editados")
        val LEGACY_HIDDEN_APPS = stringSetPreferencesKey("apps_ocultas")
    }

    val settings: Flow<AppSettings> = store.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { p ->
            val tvs = readTvs(p)
            AppSettings(
                tvs = tvs,
                selectedTvId = p[Keys.SELECTED_TV]?.takeIf { id -> tvs.any { it.id == id } } ?: tvs.first().id,
                keepScreenOn = p[Keys.KEEP_SCREEN_ON] ?: false,
                wizardDone = p[Keys.WIZARD_DONE] ?: false,
            )
        }

    private fun readTvs(p: Preferences): List<TvConfig> {
        decodeTvs(p[Keys.TVS])?.let { return it }
        // Primera vez con esta versión: lo configurado antes queda en la LG de la cocina.
        val cocina = DEFAULT_TVS[0].copy(
            profileId = profileById(p[Keys.LEGACY_PROFILE]).id,
            overrides = decodeOverrides(p[Keys.LEGACY_OVERRIDES]),
        )
        return listOf(cocina) + DEFAULT_TVS.drop(1)
    }

    private fun selectedId(p: Preferences, tvs: List<TvConfig>): String =
        p[Keys.SELECTED_TV]?.takeIf { id -> tvs.any { it.id == id } } ?: tvs.first().id

    /** Cambia la tele elegida (o la indicada por [tvId]). */
    private suspend fun editTv(tvId: String? = null, change: (TvConfig) -> TvConfig) {
        store.edit { p ->
            val tvs = readTvs(p)
            val target = tvId ?: selectedId(p, tvs)
            p.writeTvs(tvs.map { if (it.id == target) change(it) else it })
        }
    }

    private fun MutablePreferences.writeTvs(tvs: List<TvConfig>) {
        this[Keys.TVS] = encodeTvs(tvs)
    }

    suspend fun selectTv(tvId: String) {
        store.edit { it[Keys.SELECTED_TV] = tvId }
    }

    suspend fun renameTv(tvId: String, name: String) {
        val clean = name.trim()
        if (clean.isNotEmpty()) editTv(tvId) { it.copy(name = clean) }
    }

    /** Agrega una tele nueva y la deja elegida. */
    suspend fun addTv(name: String, profileId: String) {
        store.edit { p ->
            val tvs = readTvs(p)
            val tv = TvConfig(
                id = UUID.randomUUID().toString(),
                name = name.trim().ifEmpty { "Tele ${tvs.size + 1}" },
                profileId = profileById(profileId).id,
            )
            p.writeTvs(tvs + tv)
            p[Keys.SELECTED_TV] = tv.id
        }
    }

    /** Borra una tele (siempre queda al menos una). */
    suspend fun deleteTv(tvId: String) {
        store.edit { p ->
            val tvs = readTvs(p)
            if (tvs.size <= 1) return@edit
            val rest = tvs.filter { it.id != tvId }
            p.writeTvs(rest)
            if (selectedId(p, tvs) == tvId) p[Keys.SELECTED_TV] = rest.first().id
        }
    }

    suspend fun setProfile(profileId: String) {
        editTv { it.copy(profileId = profileById(profileId).id, protocolOverride = null, foundRemote = null) }
    }

    /** Guarda en la tele elegida todos los botones de un control encontrado con la Búsqueda automática. */
    suspend fun applyCodeSet(set: CodeSet) {
        editTv { tv ->
            val mine = HashMap<String, String>()
            for ((key, code) in set.codes) mine[key.name] = formatHexCode(code)
            tv.copy(
                profileId = PROFILE_BLANK,
                overrides = tv.overrides + (PROFILE_BLANK to mine),
                protocolOverride = set.protocol,
                foundRemote = "${set.brand} · ${set.model}",
            )
        }
    }

    suspend fun setWizardDone(done: Boolean) {
        store.edit { it[Keys.WIZARD_DONE] = done }
    }

    suspend fun setKeepScreenOn(on: Boolean) {
        store.edit { it[Keys.KEEP_SCREEN_ON] = on }
    }

    suspend fun setAppHidden(key: RemoteKey, hidden: Boolean) {
        editTv { tv -> tv.copy(hiddenApps = if (hidden) tv.hiddenApps + key else tv.hiddenApps - key) }
    }

    /** Guarda el código de un botón. Si es igual al de fábrica, se borra el cambio. */
    suspend fun setCode(profileId: String, key: RemoteKey, code: Long?) {
        val profile = profileById(profileId)
        editTv { tv ->
            val all = tv.overrides.toMutableMap()
            val mine = all[profile.id].orEmpty().toMutableMap()
            if (code == profile.codes[key]) {
                mine.remove(key.name)
            } else {
                mine[key.name] = code?.let(::formatHexCode) ?: ""
            }
            all[profile.id] = mine
            tv.copy(overrides = all)
        }
    }

    /** Restaurar valores de fábrica del perfil (solo en la tele elegida). */
    suspend fun resetProfile(profileId: String) {
        editTv { tv -> tv.copy(overrides = tv.overrides - profileById(profileId).id) }
    }

    /** Importa un archivo de configuración en la tele elegida. */
    suspend fun applyImport(config: ImportedConfig) {
        val profile = profileById(config.profileId)
        editTv { tv ->
            val mine = HashMap<String, String>()
            for ((key, code) in config.codes) {
                if (code != profile.codes[key]) mine[key.name] = code?.let(::formatHexCode) ?: ""
            }
            tv.copy(
                profileId = profile.id,
                overrides = tv.overrides + (profile.id to mine),
                hiddenApps = config.hiddenApps,
            )
        }
    }
}

private fun encodeTvs(tvs: List<TvConfig>): String {
    val array = JSONArray()
    for (tv in tvs) {
        val hidden = JSONArray()
        tv.hiddenApps.sortedBy { it.ordinal }.forEach { hidden.put(it.name) }
        array.put(
            JSONObject()
                .put("id", tv.id)
                .put("nombre", tv.name)
                .put("perfil", tv.profileId)
                .put("codigos", JSONObject(encodeOverrides(tv.overrides)))
                .put("appsOcultas", hidden)
                .put("protocolo", tv.protocolOverride?.name ?: "")
                .put("controlEncontrado", tv.foundRemote ?: ""),
        )
    }
    return array.toString()
}

/** null si no hay teles guardadas todavía (o el dato está dañado). */
private fun decodeTvs(text: String?): List<TvConfig>? {
    if (text.isNullOrBlank()) return null
    return try {
        val array = JSONArray(text)
        val tvs = (0 until array.length()).mapNotNull { i ->
            val obj = array.optJSONObject(i) ?: return@mapNotNull null
            val hidden = obj.optJSONArray("appsOcultas")
            TvConfig(
                id = obj.optString("id").ifEmpty { UUID.randomUUID().toString() },
                name = obj.optString("nombre").ifEmpty { "Tele ${i + 1}" }
                    // Nombre antiguo de fábrica → nombre nuevo (si nadie lo cambió a mano).
                    .let { if (it == "TV Pipi pieza") "Pieza Pipi" else it },
                profileId = profileById(obj.optString("perfil")).id,
                overrides = decodeOverrides(obj.optJSONObject("codigos")?.toString()),
                hiddenApps = if (hidden == null) {
                    DEFAULT_HIDDEN_APPS
                } else {
                    (0 until hidden.length())
                        .mapNotNull { j -> APP_KEYS.firstOrNull { it.name == hidden.optString(j) } }
                        .toSet()
                },
                protocolOverride = IrProtocol.entries.firstOrNull { it.name == obj.optString("protocolo") },
                foundRemote = obj.optString("controlEncontrado").ifEmpty { null },
            )
        }
        tvs.map(::upgradeFoundAoc).ifEmpty { null }
    } catch (e: JSONException) {
        null
    }
}

/**
 * Si la Búsqueda automática encontró el monitor Philips 22IT (misma señal 00BD que la AOC),
 * se pasa al perfil completo «AOC Smart TV (TPV)», conservando los botones que el usuario guardó.
 */
private fun upgradeFoundAoc(tv: TvConfig): TvConfig {
    if (tv.profileId != PROFILE_BLANK || tv.protocolOverride != IrProtocol.NEC) return tv
    if (tv.foundRemote?.contains("Philips_22IT") != true) return tv
    val aoc = profileById(PROFILE_AOC)
    val mine = tv.overrides[PROFILE_BLANK].orEmpty().filter { (key, value) ->
        val factory = RemoteKey.entries.firstOrNull { it.name == key }?.let { aoc.codes[it] }
        value.isNotEmpty() && parseHexCode(value) != factory
    }
    return tv.copy(
        profileId = PROFILE_AOC,
        overrides = tv.overrides + (PROFILE_AOC to mine),
        protocolOverride = null,
        foundRemote = null,
    )
}
