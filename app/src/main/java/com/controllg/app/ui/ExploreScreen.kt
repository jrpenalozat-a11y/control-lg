package com.controllg.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.controllg.app.APP_KEYS
import com.controllg.app.IrProtocol
import com.controllg.app.NUMBER_KEYS
import com.controllg.app.RemoteKey
import com.controllg.app.brandOf
import com.controllg.app.candidateCode
import com.controllg.app.commandCount
import com.controllg.app.formatHexCode
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Botones que se pueden asignar al explorar, en el orden en que se muestran. */
private val EXPLORE_KEYS: List<RemoteKey> = listOf(
    RemoteKey.HOME, RemoteKey.SETTINGS, RemoteKey.BACK, RemoteKey.EXIT,
    RemoteKey.OK, RemoteKey.UP, RemoteKey.DOWN, RemoteKey.LEFT, RemoteKey.RIGHT,
    RemoteKey.INPUT, RemoteKey.VOL_UP, RemoteKey.VOL_DOWN, RemoteKey.MUTE,
    RemoteKey.CH_UP, RemoteKey.CH_DOWN,
) + APP_KEYS + NUMBER_KEYS

private fun shortLabel(key: RemoteKey): String = when (key) {
    RemoteKey.SETTINGS -> "Ajustes TV"
    RemoteKey.VOL_UP -> "Vol +"
    RemoteKey.VOL_DOWN -> "Vol −"
    RemoteKey.CH_UP -> "Canal +"
    RemoteKey.CH_DOWN -> "Canal −"
    RemoteKey.PRIME -> "Prime"
    else -> key.label
}

/**
 * Explorar códigos: recorre los 256 comandos de la «marca» de la tele y, cuando la tele hace algo,
 * el usuario dice qué fue («Esto fue: Home») y se guarda en ese botón. Una pasada configura muchos.
 */
@Composable
fun ExploreScreen(
    tvName: String,
    protocol: IrProtocol,
    codes: Map<RemoteKey, Long?>,
    onTest: suspend (Long) -> Unit,
    onSetCode: (RemoteKey, Long) -> Unit,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val base = codes[RemoteKey.POWER] ?: codes.values.firstOrNull { it != null }
    val brand = remember(base, protocol) { if (base != null) brandOf(protocol, base) else 0L }
    val total = commandCount(protocol)
    var index by rememberSaveable { mutableIntStateOf(-1) }
    var auto by rememberSaveable { mutableStateOf(false) }
    var lastSaved by remember { mutableStateOf<String?>(null) }

    fun candidate(i: Int): Long = candidateCode(protocol, brand, i)
    val power = codes[RemoteKey.POWER]

    /** Salta el código de encendido, para no apagar la tele mientras se explora. */
    fun step(from: Int, dir: Int): Int {
        var i = from + dir
        while (i in 0 until total && candidate(i) == power) i += dir
        return i
    }

    fun sendCurrent() {
        if (index in 0 until total) scope.launch { onTest(candidate(index)) }
    }

    LaunchedEffect(auto) {
        while (auto) {
            index = step(index, 1)
            if (index >= total) {
                index = total - 1
                auto = false
            } else {
                sendCurrent()
                delay(3000)
            }
        }
    }

    val current = if (index in 0 until total) candidate(index) else null
    val assigned = remember(codes) { codes.entries.filter { it.value != null }.associate { it.value to it.key } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Header(title = "Explorar códigos", onBack = onBack)
        Text(
            "Tele: $tvName. Con la tele encendida, toca «Automático». Cuando la tele haga algo " +
                "(abrir un menú, ir al inicio, abrir YouTube…), toca «Pausar», confirma con «Repetir» " +
                "o «Anterior», y abajo toca qué fue. Luego sigue. El de encendido se salta.",
            fontSize = 14.sp,
            lineHeight = 20.sp,
            color = MaterialTheme.colorScheme.onBackground,
        )
        LinearProgressIndicator(
            progress = { ((index + 1).coerceAtLeast(0)) / total.toFloat() },
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            if (current != null) {
                val who = assigned[current]?.let { " · ya es «${shortLabel(it)}»" } ?: ""
                "Código ${index + 1} de $total: ${formatHexCode(current)}$who"
            } else {
                "Todavía no se envía ningún código."
            },
            fontFamily = FontFamily.Monospace,
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onBackground,
        )
        lastSaved?.let {
            Text(it, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
        }

        Button(
            enabled = index < total - 1 || auto,
            onClick = { auto = !auto },
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
        ) { Text(if (auto) "Pausar" else if (index < 0) "Automático" else "Seguir automático") }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedButton(
                enabled = !auto && index > 0,
                onClick = {
                    index = step(index, -1).coerceAtLeast(0)
                    sendCurrent()
                },
                modifier = Modifier.weight(1f).heightIn(min = 48.dp),
            ) { Text("Anterior", maxLines = 1) }
            OutlinedButton(
                enabled = !auto && current != null,
                onClick = { sendCurrent() },
                modifier = Modifier.weight(1f).heightIn(min = 48.dp),
            ) { Text("Repetir", maxLines = 1) }
            OutlinedButton(
                enabled = !auto && index < total - 1,
                onClick = {
                    index = step(index, 1)
                    sendCurrent()
                },
                modifier = Modifier.weight(1f).heightIn(min = 48.dp),
            ) { Text("Siguiente", maxLines = 1) }
        }

        SectionTitle("Esto fue…")
        EXPLORE_KEYS.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { key ->
                    val has = codes[key] != null
                    OutlinedButton(
                        enabled = !auto && current != null,
                        onClick = {
                            val code = current ?: return@OutlinedButton
                            onSetCode(key, code)
                            lastSaved = "Guardado: «${shortLabel(key)}» = ${formatHexCode(code)}"
                        },
                        modifier = Modifier.weight(1f).heightIn(min = 44.dp),
                    ) {
                        Text(
                            (if (has) "✓ " else "") + shortLabel(key),
                            fontSize = 13.sp,
                            maxLines = 1,
                        )
                    }
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}
