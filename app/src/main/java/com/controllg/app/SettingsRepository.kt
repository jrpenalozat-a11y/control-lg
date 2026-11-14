package com.controllg.app

import android.content.Context
import androidx.datastore.core.DataStore
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
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "ajustes")

data class AppSettings(
    val profileId: String = PROFILE_STANDARD,
    /** Códigos cambiados en el editor, por perfil: BOTON → "0x…" o "" (sin código). */
    val overrides: Map<String, Map<String, String>> = emptyMap(),
    val hiddenApps: Set<RemoteKey> = DEFAULT_HIDDEN_APPS,
    val keepScreenOn: Boolean = false,
    val wizardDone: Boolean = false,
) {
    val profile: CodeProfile get() = profileById(profileId)

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
        val PROFILE = stringPreferencesKey("perfil")
        val OVERRIDES = stringPreferencesKey("codigos_editados")
        val HIDDEN_APPS = stringSetPreferencesKey("apps_ocultas")
        val KEEP_SCREEN_ON = booleanPreferencesKey("pantalla_encendida")
        val WIZARD_DONE = booleanPreferencesKey("asistente_listo")
    }

    val settings: Flow<AppSettings> = store.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { p ->
            AppSettings(
                profileId = profileById(p[Keys.PROFILE]).id,
                overrides = decodeOverrides(p[Keys.OVERRIDES]),
                hiddenApps = p[Keys.HIDDEN_APPS]
                    ?.mapNotNull { name -> APP_KEYS.firstOrNull { it.name == name } }
                    ?.toSet()
                    ?: DEFAULT_HIDDEN_APPS,
                keepScreenOn = p[Keys.KEEP_SCREEN_ON] ?: false,
                wizardDone = p[Keys.WIZARD_DONE] ?: false,
            )
        }

    suspend fun setProfile(profileId: String) {
        store.edit { it[Keys.PROFILE] = profileById(profileId).id }
    }

    suspend fun setWizardDone(done: Boolean) {
        store.edit { it[Keys.WIZARD_DONE] = done }
    }

    suspend fun setKeepScreenOn(on: Boolean) {
        store.edit { it[Keys.KEEP_SCREEN_ON] = on }
    }

    suspend fun setAppHidden(key: RemoteKey, hidden: Boolean) {
        store.edit { p ->
            val current = p[Keys.HIDDEN_APPS] ?: DEFAULT_HIDDEN_APPS.map { it.name }.toSet()
            p[Keys.HIDDEN_APPS] = if (hidden) current + key.name else current - key.name
        }
    }

    /** Guarda el código de un botón. Si es igual al de fábrica, se borra el cambio. */
    suspend fun setCode(profileId: String, key: RemoteKey, code: Long?) {
        val profile = profileById(profileId)
        store.edit { p ->
            val all = decodeOverrides(p[Keys.OVERRIDES]).toMutableMap()
            val mine = all[profile.id].orEmpty().toMutableMap()
            if (code == profile.codes[key]) {
                mine.remove(key.name)
            } else {
                mine[key.name] = code?.let(::formatHexCode) ?: ""
            }
            all[profile.id] = mine
            p[Keys.OVERRIDES] = encodeOverrides(all)
        }
    }

    /** Restaurar valores de fábrica del perfil. */
    suspend fun resetProfile(profileId: String) {
        store.edit { p ->
            val all = decodeOverrides(p[Keys.OVERRIDES]).toMutableMap()
            all.remove(profileById(profileId).id)
            p[Keys.OVERRIDES] = encodeOverrides(all)
        }
    }

    suspend fun applyImport(config: ImportedConfig) {
        val profile = profileById(config.profileId)
        store.edit { p ->
            val all = decodeOverrides(p[Keys.OVERRIDES]).toMutableMap()
            val mine = HashMap<String, String>()
            for ((key, code) in config.codes) {
                if (code != profile.codes[key]) mine[key.name] = code?.let(::formatHexCode) ?: ""
            }
            all[profile.id] = mine
            p[Keys.PROFILE] = profile.id
            p[Keys.OVERRIDES] = encodeOverrides(all)
            p[Keys.HIDDEN_APPS] = config.hiddenApps.map { it.name }.toSet()
        }
    }
}
