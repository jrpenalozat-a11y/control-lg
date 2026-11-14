package com.controllg.app

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/** Lo que trae un archivo de configuración importado. */
data class ImportedConfig(
    val profileId: String,
    val codes: Map<RemoteKey, Long?>,
    val hiddenApps: Set<RemoteKey>,
)

/** Archivo JSON para compartir la configuración entre celulares. */
fun exportConfigJson(settings: AppSettings): String {
    val codigos = JSONObject()
    for ((key, code) in settings.effectiveCodes()) {
        codigos.put(key.name, code?.let(::formatHexCode) ?: "")
    }
    val ocultas = JSONArray()
    settings.hiddenApps.sortedBy { it.ordinal }.forEach { ocultas.put(it.name) }
    return JSONObject()
        .put("app", "Control LG")
        .put("version", 1)
        .put("perfil", settings.profileId)
        .put("codigos", codigos)
        .put("appsOcultas", ocultas)
        .toString(2)
}

/** Lee un archivo exportado. Lanza IllegalArgumentException con un mensaje claro si no sirve. */
fun parseConfigJson(text: String): ImportedConfig {
    val root = try {
        JSONObject(text)
    } catch (e: JSONException) {
        throw IllegalArgumentException("el archivo no es un JSON válido")
    }
    val codigos = root.optJSONObject("codigos")
        ?: throw IllegalArgumentException("el archivo no trae la sección «codigos»")
    val profile = profileById(root.optString("perfil"))

    val codes = LinkedHashMap<RemoteKey, Long?>()
    for (key in profile.codes.keys) {
        if (!codigos.has(key.name)) continue
        val raw = codigos.optString(key.name).trim()
        codes[key] = if (raw.isEmpty()) {
            null
        } else {
            parseHexCode(raw)
                ?: throw IllegalArgumentException("código inválido en «${key.label}»: $raw")
        }
    }
    if (codes.isEmpty()) throw IllegalArgumentException("el archivo no trae ningún código conocido")

    val hidden = HashSet<RemoteKey>()
    val ocultas = root.optJSONArray("appsOcultas")
    if (ocultas != null) {
        for (i in 0 until ocultas.length()) {
            val name = ocultas.optString(i)
            APP_KEYS.firstOrNull { it.name == name }?.let { hidden.add(it) }
        }
    }
    return ImportedConfig(profile.id, codes, hidden)
}

/** Cambios hechos en el editor: { perfil: { BOTON: "0x…" o "" (sin código) } }. */
fun encodeOverrides(overrides: Map<String, Map<String, String>>): String {
    val root = JSONObject()
    for ((profileId, keys) in overrides) {
        if (keys.isEmpty()) continue
        val obj = JSONObject()
        for ((key, value) in keys) obj.put(key, value)
        root.put(profileId, obj)
    }
    return root.toString()
}

fun decodeOverrides(text: String?): Map<String, Map<String, String>> {
    if (text.isNullOrBlank()) return emptyMap()
    return try {
        val root = JSONObject(text)
        val result = HashMap<String, Map<String, String>>()
        for (profileId in root.keys()) {
            val obj = root.optJSONObject(profileId) ?: continue
            val keys = HashMap<String, String>()
            for (key in obj.keys()) keys[key] = obj.optString(key)
            result[profileId] = keys
        }
        result
    } catch (e: JSONException) {
        emptyMap()
    }
}
