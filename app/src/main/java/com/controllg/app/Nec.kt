package com.controllg.app

import java.util.Locale

/** Frecuencia portadora del protocolo NEC que usan los televisores LG. */
const val NEC_CARRIER_HZ = 38_000

private const val HEADER_ON = 9000
private const val HEADER_OFF = 4500
private const val BIT_ON = 560
private const val ONE_OFF = 1690
private const val ZERO_OFF = 560

/**
 * Arma el patrón (en microsegundos, encendido/apagado alternados) de un código NEC de 32 bits
 * para ConsumerIrManager.transmit(). Los bits se envían del más significativo al menos.
 */
fun encodeNec(code: Long): IntArray {
    val pattern = IntArray(2 + 32 * 2 + 1)
    pattern[0] = HEADER_ON
    pattern[1] = HEADER_OFF
    var i = 2
    for (bit in 31 downTo 0) {
        val isOne = ((code shr bit) and 1L) == 1L
        pattern[i++] = BIT_ON
        pattern[i++] = if (isOne) ONE_OFF else ZERO_OFF
    }
    pattern[i] = BIT_ON
    return pattern
}

/** "0x20DF10EF" o "20df10ef" → 0x20DF10EF. Devuelve null si no son 8 dígitos hexadecimales. */
fun parseHexCode(text: String): Long? {
    val clean = text.trim().removePrefix("0x").removePrefix("0X")
    if (clean.length != 8) return null
    if (!clean.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }) return null
    return clean.toLong(16)
}

fun formatHexCode(code: Long): String = String.format(Locale.ROOT, "0x%08X", code)
