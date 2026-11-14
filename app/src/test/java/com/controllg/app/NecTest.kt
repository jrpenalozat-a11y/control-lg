package com.controllg.app

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NecTest {

    @Test
    fun patron_tiene_cabecera_32_bits_y_cierre() {
        val p = encodeNec(0x20DF10EFL)
        assertEquals(2 + 64 + 1, p.size)
        assertEquals(9000, p[0])
        assertEquals(4500, p[1])
        assertEquals(560, p.last())
        // Todos los tiempos de "encendido" de los bits son 560
        for (i in 2 until p.size step 2) assertEquals(560, p[i])
    }

    @Test
    fun bits_van_del_mas_significativo_al_menos() {
        // 0x20DF10EF = 0010 0000 1101 1111 0001 0000 1110 1111
        val bits = "00100000110111110001000011101111"
        val p = encodeNec(0x20DF10EFL)
        val esperado = bits.map { if (it == '1') 1690 else 560 }.toIntArray()
        val obtenido = IntArray(32) { p[2 + it * 2 + 1] }
        assertArrayEquals(esperado, obtenido)
    }

    @Test
    fun extremos() {
        val ceros = encodeNec(0x00000000L)
        val unos = encodeNec(0xFFFFFFFFL)
        for (b in 0 until 32) {
            assertEquals(560, ceros[2 + b * 2 + 1])
            assertEquals(1690, unos[2 + b * 2 + 1])
        }
    }

    @Test
    fun hexadecimal_ida_y_vuelta() {
        assertEquals(0x20DF10EFL, parseHexCode("0x20DF10EF"))
        assertEquals(0x20DF10EFL, parseHexCode(" 20df10ef "))
        assertEquals(0xFFFFFFFFL, parseHexCode("FFFFFFFF"))
        assertNull(parseHexCode("0x20DF"))
        assertNull(parseHexCode("0x20DF10EG"))
        assertNull(parseHexCode(""))
        assertEquals("0x20DF10EF", formatHexCode(0x20DF10EFL))
        assertEquals("0x00FF00FF", formatHexCode(0x00FF00FFL))
    }

    @Test
    fun perfiles_tienen_los_codigos_pedidos() {
        val estandar = profileById(PROFILE_STANDARD)
        val alternativo = profileById(PROFILE_ALT)
        assertEquals(0x20DF10EFL, estandar.codes[RemoteKey.POWER])
        assertEquals(null, estandar.codes[RemoteKey.POWER_ON])
        assertEquals(0x20DF10EFL, alternativo.codes[RemoteKey.POWER])
        assertEquals(0x20DF23DCL, alternativo.codes[RemoteKey.POWER_ON])
        assertEquals(0x20DFA35CL, alternativo.codes[RemoteKey.POWER_OFF])
        assertEquals(estandar.codes[RemoteKey.VOL_UP], alternativo.codes[RemoteKey.VOL_UP])
    }
}
