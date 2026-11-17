package com.controllg.app

/** Formato de la señal infrarroja de cada perfil. */
enum class IrProtocol(val carrierHz: Int) {
    /** LG, Roku y la mayoría de las teles: códigos de 32 bits (0x20DF10EF). */
    NEC(NEC_CARRIER_HZ),

    /** Philips y teles hechas por TPV (AOC): código = dirección·256 + comando (0x0000000C). */
    RC6(36_000),

    /** Philips más antiguas: código = dirección (5 bits)·256 + comando (7 bits). */
    RC5(36_000),

    /** Samsung: igual que NEC pero con otra cabecera (0xE0E040BF). */
    SAMSUNG(38_000),
}

/** Cómo se busca un código: cuántos comandos hay y cómo se arma con la «marca» (4 dígitos). */
fun candidateCode(protocol: IrProtocol, brand: Long, command: Int): Long = when (protocol) {
    IrProtocol.NEC, IrProtocol.SAMSUNG ->
        (brand shl 16) or (command.toLong() shl 8) or (command.inv().toLong() and 0xFF)
    IrProtocol.RC6 -> ((brand and 0xFF) shl 8) or command.toLong()
    IrProtocol.RC5 -> ((brand and 0x1F) shl 8) or (command.toLong() and 0x7F)
}

fun commandCount(protocol: IrProtocol): Int = if (protocol == IrProtocol.RC5) 128 else 256

/** «Marca» (4 dígitos del buscador) a partir de un código ya conocido. */
fun brandOf(protocol: IrProtocol, code: Long): Long = when (protocol) {
    IrProtocol.NEC, IrProtocol.SAMSUNG -> (code shr 16) and 0xFFFF
    IrProtocol.RC6, IrProtocol.RC5 -> (code shr 8) and 0xFF
}

/** Patrón (µs) para enviar [code] con [protocol]. [toggle] solo lo usan RC5 y RC6. */
fun encodeIr(protocol: IrProtocol, code: Long, toggle: Boolean): IntArray = when (protocol) {
    IrProtocol.NEC -> encodeNec(code)
    IrProtocol.SAMSUNG -> encodeSamsung(code)
    IrProtocol.RC6 -> encodeRc6(code, toggle)
    IrProtocol.RC5 -> encodeRc5(code, toggle)
}

/** Samsung32: cabecera 4500/4500 y luego 32 bits como NEC. */
fun encodeSamsung(code: Long): IntArray = encodeNec(code).also {
    it[0] = 4500
    it[1] = 4500
}

/** Junta tramos seguidos del mismo nivel, quita espacios al inicio/fin y deja solo duraciones. */
private fun toPattern(parts: List<Pair<Boolean, Int>>): IntArray {
    val merged = ArrayList<Pair<Boolean, Int>>()
    for (part in parts) {
        val last = merged.lastOrNull()
        if (last != null && last.first == part.first) {
            merged[merged.size - 1] = last.first to last.second + part.second
        } else {
            merged.add(part)
        }
    }
    while (merged.isNotEmpty() && !merged.first().first) merged.removeAt(0)
    while (merged.isNotEmpty() && !merged.last().first) merged.removeAt(merged.size - 1)
    return merged.map { it.second }.toIntArray()
}

private const val RC5_UNIT = 889

/**
 * RC5 (36 kHz): 14 bits Manchester — inicio, campo (comando ≥ 64 lo invierte), repetición,
 * 5 de dirección y 6 de comando. En RC5 un 1 es apagado y luego encendido.
 */
fun encodeRc5(code: Long, toggle: Boolean): IntArray {
    val address = ((code shr 8) and 0x1F).toInt()
    val command = (code and 0x7F).toInt()
    val bits = ArrayList<Boolean>()
    bits.add(true)
    bits.add(command < 64)
    bits.add(toggle)
    for (i in 4 downTo 0) bits.add(((address shr i) and 1) == 1)
    for (i in 5 downTo 0) bits.add(((command shr i) and 1) == 1)
    val parts = ArrayList<Pair<Boolean, Int>>()
    for (one in bits) {
        parts.add(!one to RC5_UNIT)
        parts.add(one to RC5_UNIT)
    }
    return toPattern(parts)
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
    return toPattern(parts)
}
