package com.controllg.app

import android.content.Context
import org.json.JSONObject

/**
 * Un control remoto completo de la base pública Flipper-IRDB (dominio público).
 * Lo usa la «Búsqueda automática»: se envía su [power] y, si la tele responde, se guardan sus [codes].
 */
data class CodeSet(
    val brand: String,
    val model: String,
    val protocol: IrProtocol,
    val codes: Map<RemoteKey, Long>,
) {
    val power: Long get() = codes.getValue(RemoteKey.POWER)
}

/** Lee assets/teles_ir.json (lo genera tools/gen_ir_tvs.py). */
fun loadCodeSets(context: Context): List<CodeSet> =
    context.assets.open("teles_ir.json").use { parseCodeSets(it.readBytes().toString(Charsets.UTF_8)) }

fun parseCodeSets(text: String): List<CodeSet> {
    val teles = JSONObject(text).getJSONArray("teles")
    return (0 until teles.length()).mapNotNull { i ->
        val obj = teles.getJSONObject(i)
        val protocol = when (obj.optString("protocolo")) {
            "NEC" -> IrProtocol.NEC
            "RC6" -> IrProtocol.RC6
            "RC5" -> IrProtocol.RC5
            "SAMSUNG" -> IrProtocol.SAMSUNG
            else -> return@mapNotNull null
        }
        val raw = obj.getJSONObject("codigos")
        val codes = LinkedHashMap<RemoteKey, Long>()
        for (name in raw.keys()) {
            val key = RemoteKey.entries.firstOrNull { it.name == name } ?: continue
            parseHexCode(raw.getString(name))?.let { codes[key] = it }
        }
        if (RemoteKey.POWER !in codes) return@mapNotNull null
        CodeSet(obj.optString("marca"), obj.optString("control"), protocol, codes)
    }
}
