package com.controllg.app

/**
 * Códigos infrarrojos de fábrica (NEC, 32 bits) para televisores LG.
 * Se pueden cambiar aquí, o sin recompilar desde Configuración → Editor de códigos.
 */
enum class RemoteKey(val label: String) {
    POWER("Encender/Apagar"),
    POWER_ON("Encender (discreto)"),
    POWER_OFF("Apagar (discreto)"),
    VOL_UP("Volumen +"),
    VOL_DOWN("Volumen −"),
    MUTE("Silencio"),
    CH_UP("Canal +"),
    CH_DOWN("Canal −"),
    INPUT("Entrada"),
    HOME("Home"),
    SETTINGS("Configuración de la tele"),
    UP("Arriba"),
    DOWN("Abajo"),
    LEFT("Izquierda"),
    RIGHT("Derecha"),
    OK("OK"),
    BACK("Atrás"),
    EXIT("Salir"),
    NETFLIX("Netflix"),
    PRIME("Prime Video"),
    DISNEY("Disney+"),
    YOUTUBE("YouTube"),
    MAX("Max"),
    SPOTIFY("Spotify"),
    NUM_1("1"),
    NUM_2("2"),
    NUM_3("3"),
    NUM_4("4"),
    NUM_5("5"),
    NUM_6("6"),
    NUM_7("7"),
    NUM_8("8"),
    NUM_9("9"),
    NUM_0("0"),
}

/** Botones de apps: se pueden ocultar desde Configuración. */
val APP_KEYS = listOf(
    RemoteKey.NETFLIX, RemoteKey.PRIME, RemoteKey.DISNEY,
    RemoteKey.YOUTUBE, RemoteKey.MAX, RemoteKey.SPOTIFY,
)

/** Todas las apps visibles de partida; las que no tengan código avisan al tocarlas. */
val DEFAULT_HIDDEN_APPS = emptySet<RemoteKey>()

val NUMBER_KEYS = listOf(
    RemoteKey.NUM_1, RemoteKey.NUM_2, RemoteKey.NUM_3,
    RemoteKey.NUM_4, RemoteKey.NUM_5, RemoteKey.NUM_6,
    RemoteKey.NUM_7, RemoteKey.NUM_8, RemoteKey.NUM_9,
    RemoteKey.NUM_0,
)

/** null = el botón no tiene código de fábrica (se agrega desde el editor). */
private val STANDARD_CODES: Map<RemoteKey, Long?> = linkedMapOf(
    RemoteKey.POWER to 0x20DF10EFL,
    RemoteKey.VOL_UP to 0x20DF40BFL,
    RemoteKey.VOL_DOWN to 0x20DFC03FL,
    RemoteKey.MUTE to 0x20DF906FL,
    RemoteKey.CH_UP to 0x20DF00FFL,
    RemoteKey.CH_DOWN to 0x20DF807FL,
    RemoteKey.INPUT to 0x20DFD02FL,
    RemoteKey.HOME to 0x20DF3EC1L,
    RemoteKey.SETTINGS to 0x20DFC23DL,
    RemoteKey.UP to 0x20DF02FDL,
    RemoteKey.DOWN to 0x20DF827DL,
    RemoteKey.LEFT to 0x20DFE01FL,
    RemoteKey.RIGHT to 0x20DF609FL,
    RemoteKey.OK to 0x20DF22DDL,
    RemoteKey.BACK to 0x20DF14EBL,
    RemoteKey.EXIT to 0x20DFDA25L,
    RemoteKey.NETFLIX to 0x20DF6A95L,
    RemoteKey.PRIME to 0x20DF3AC5L,
    RemoteKey.DISNEY to null,
    RemoteKey.YOUTUBE to null,
    RemoteKey.MAX to null,
    RemoteKey.SPOTIFY to null,
    RemoteKey.NUM_1 to 0x20DF8877L,
    RemoteKey.NUM_2 to 0x20DF48B7L,
    RemoteKey.NUM_3 to 0x20DFC837L,
    RemoteKey.NUM_4 to 0x20DF28D7L,
    RemoteKey.NUM_5 to 0x20DFA857L,
    RemoteKey.NUM_6 to 0x20DF6897L,
    RemoteKey.NUM_7 to 0x20DFE817L,
    RemoteKey.NUM_8 to 0x20DF18E7L,
    RemoteKey.NUM_9 to 0x20DF9867L,
    RemoteKey.NUM_0 to 0x20DF08F7L,
)

/** Mismo set, más Encender y Apagar por separado (códigos discretos). */
private val ALT_CODES: Map<RemoteKey, Long?> = LinkedHashMap<RemoteKey, Long?>().apply {
    for ((key, code) in STANDARD_CODES) {
        put(key, code)
        if (key == RemoteKey.POWER) {
            put(RemoteKey.POWER_ON, 0x20DF23DCL)
            put(RemoteKey.POWER_OFF, 0x20DFA35CL)
        }
    }
}

data class CodeProfile(
    val id: String,
    val name: String,
    val description: String,
    /** Botones del perfil, en el orden en que aparecen en el editor. */
    val codes: Map<RemoteKey, Long?>,
    /** Botones que envía el asistente «Probar mi tele». */
    val testKeys: List<RemoteKey>,
    val protocol: IrProtocol = IrProtocol.NEC,
)

const val PROFILE_STANDARD = "lg_estandar"
const val PROFILE_ALT = "lg_alternativo"
const val PROFILE_BLANK = "por_configurar"
const val PROFILE_ROKU = "roku_tv"
const val PROFILE_TPV = "aoc_philips_rc6"

/** Mismos botones que LG estándar, todos sin código: para teles de otra marca (p. ej. AOC). */
private val BLANK_CODES: Map<RemoteKey, Long?> = STANDARD_CODES.mapValues { null }

/**
 * Roku TV (TCL, Hisense, AOC Roku…): mismo formato NEC, dirección 57E3.
 * Códigos grabados de un control Roku (github.com/warlock31415/Roku-Remote).
 * Roku no tiene Entrada, canales ni números: quedan vacíos y se pueden buscar.
 */
private val ROKU_CODES: Map<RemoteKey, Long?> = BLANK_CODES + linkedMapOf(
    RemoteKey.POWER to 0x57E3E817L,
    RemoteKey.VOL_UP to 0x57E3F00FL,
    RemoteKey.VOL_DOWN to 0x57E308F7L,
    RemoteKey.MUTE to 0x57E304FBL,
    RemoteKey.HOME to 0x57E3C03FL,
    RemoteKey.UP to 0x57E39867L,
    RemoteKey.DOWN to 0x57E3CC33L,
    RemoteKey.LEFT to 0x57E37887L,
    RemoteKey.RIGHT to 0x57E3B44BL,
    RemoteKey.OK to 0x57E354ABL,
    RemoteKey.BACK to 0x57E36699L,
)

/**
 * AOC / Philips latinoamericanas (TPV), protocolo RC6, dirección 0: código = comando.
 * Tomados del control Philips 50PUT6103/79 y series 7956/OLED806 (Flipper-IRDB).
 * La AOC 32S5295 es de la misma fábrica; si algún botón no responde, se busca en el Editor.
 */
private val TPV_CODES: Map<RemoteKey, Long?> = BLANK_CODES + linkedMapOf(
    RemoteKey.POWER to 0x0CL,
    RemoteKey.VOL_UP to 0x10L,
    RemoteKey.VOL_DOWN to 0x11L,
    RemoteKey.MUTE to 0x0DL,
    RemoteKey.CH_UP to 0x20L,
    RemoteKey.CH_DOWN to 0x21L,
    RemoteKey.INPUT to 0x38L,
    RemoteKey.HOME to 0x54L,
    RemoteKey.SETTINGS to 0xBFL,
    RemoteKey.UP to 0x58L,
    RemoteKey.DOWN to 0x59L,
    RemoteKey.LEFT to 0x5AL,
    RemoteKey.RIGHT to 0x5BL,
    RemoteKey.OK to 0x5CL,
    RemoteKey.BACK to 0x0AL,
    RemoteKey.EXIT to 0x9FL,
    RemoteKey.NETFLIX to 0x76L,
    RemoteKey.PRIME to 0xBAL,
    RemoteKey.YOUTUBE to 0x79L,
    RemoteKey.NUM_1 to 0x01L,
    RemoteKey.NUM_2 to 0x02L,
    RemoteKey.NUM_3 to 0x03L,
    RemoteKey.NUM_4 to 0x04L,
    RemoteKey.NUM_5 to 0x05L,
    RemoteKey.NUM_6 to 0x06L,
    RemoteKey.NUM_7 to 0x07L,
    RemoteKey.NUM_8 to 0x08L,
    RemoteKey.NUM_9 to 0x09L,
    RemoteKey.NUM_0 to 0x00L,
)


val PROFILES = listOf(
    CodeProfile(
        id = PROFILE_STANDARD,
        name = "LG estándar (2008 en adelante)",
        description = "Un solo botón que enciende y apaga.",
        codes = STANDARD_CODES,
        testKeys = listOf(RemoteKey.POWER),
    ),
    CodeProfile(
        id = PROFILE_ALT,
        name = "LG alternativo",
        description = "Agrega Encender y Apagar por separado (códigos discretos).",
        codes = ALT_CODES,
        testKeys = listOf(RemoteKey.POWER_ON, RemoteKey.POWER_OFF),
    ),
    CodeProfile(
        id = PROFILE_ROKU,
        name = "Roku TV",
        description = "Teles con Roku (TCL, Hisense, AOC Roku…). Encender, volumen, flechas, OK, Atrás y Home.",
        codes = ROKU_CODES,
        testKeys = listOf(RemoteKey.POWER),
    ),
    CodeProfile(
        id = PROFILE_TPV,
        name = "AOC / Philips (RC6)",
        description = "Smart TV AOC y Philips de Latinoamérica (fabricadas por TPV). Otro tipo de señal.",
        codes = TPV_CODES,
        testKeys = listOf(RemoteKey.POWER),
        protocol = IrProtocol.RC6,
    ),
    CodeProfile(
        id = PROFILE_BLANK,
        name = "Otra marca (AOC u otra)",
        description = "Sin códigos de fábrica: se agregan con el Buscador del Editor de códigos.",
        codes = BLANK_CODES,
        testKeys = emptyList(),
    ),
)

/** Perfiles que el asistente «Probar mi tele» puede probar (tienen códigos de fábrica). */
val WIZARD_PROFILES = PROFILES.filter { p -> p.testKeys.isNotEmpty() && p.testKeys.all { p.codes[it] != null } }

fun profileById(id: String?): CodeProfile = PROFILES.firstOrNull { it.id == id } ?: PROFILES.first()
