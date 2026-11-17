package com.controllg.app

/** Formato de la señal infrarroja de cada perfil. */
enum class IrProtocol(val carrierHz: Int) {
    /** LG, Roku y la mayoría de las teles: códigos de 32 bits (0x20DF10EF). */
    NEC(NEC_CARRIER_HZ),

    /** Philips y teles hechas por TPV (AOC): código = dirección·256 + comando (0x0000000C). */
    RC6(36_000),
}

private const val RC6_UNIT = 444

/**
 * Patrón RC6 modo 0 (en microsegundos, encendido/apagado alternados) para
 * ConsumerIrManager.transmit(). [code] = dirección (8 bits) · 256 + comando (8 bits).
 * [toggle] debe alternar en cada pulsación nueva para que la tele no la tome como repetición.
 */
fun encodeRc6(code: Long, toggle: Boolean): IntArray {
    // (encendido?, duración) — luego se juntan los tramos iguales seguidos.
    val parts = ArrayList<Pair<Boolean, Int>>()
    fun bit(one: Boolean, unit: Int = RC6_UNIT) {
        // Manchester RC6: 1 = encendido y luego apagado; 0 = al revés.
        parts.add(one to unit)
        parts.add(!one to unit)
    }

    parts.add(true to RC6_UNIT * 6) // cabecera
    parts.add(false to RC6_UNIT * 2)
    bit(true) // bit de inicio
    repeat(3) { bit(false) } // modo 0
    bit(toggle, RC6_UNIT * 2) // bit de repetición, doble de largo
    for (i in 15 downTo 0) bit(((code shr i) and 1L) == 1L)

    val merged = ArrayList<Pair<Boolean, Int>>()
    for (part in parts) {
        val last = merged.lastOrNull()
        if (last != null && last.first == part.first) {
            merged[merged.size - 1] = last.first to last.second + part.second
        } else {
            merged.add(part)
        }
    }
    if (!merged.last().first) merged.removeAt(merged.size - 1) // termina en encendido
    return merged.map { it.second }.toIntArray()
}
