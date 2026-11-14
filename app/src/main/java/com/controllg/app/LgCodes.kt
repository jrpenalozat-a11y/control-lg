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
val APP_KEYS = listOf(RemoteKey.NETFLIX, RemoteKey.PRIME, RemoteKey.DISNEY, RemoteKey.YOUTUBE)

/** Apps ocultas mientras el usuario no decida otra cosa (no traen código de fábrica). */
val DEFAULT_HIDDEN_APPS = setOf(RemoteKey.DISNEY, RemoteKey.YOUTUBE)

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
)

const val PROFILE_STANDARD = "lg_estandar"
const val PROFILE_ALT = "lg_alternativo"

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
)

fun profileById(id: String?): CodeProfile = PROFILES.firstOrNull { it.id == id } ?: PROFILES.first()
