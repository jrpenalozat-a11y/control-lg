package com.controllg.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.controllg.app.CodeSet
import com.controllg.app.IrProtocol
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Segundos entre un intento y el siguiente en modo automático. */
private const val AUTO_STEP_MS = 3000L

/**
 * Búsqueda automática, como la de un control universal: envía el botón de encendido de cada
 * control de la base hasta que la tele responde, y guarda todos los botones de ese control.
 */
@Composable
fun AutoSearchScreen(
    tvName: String,
    sets: List<CodeSet>,
    onSend: suspend (Long, IrProtocol) -> Unit,
    onFound: (CodeSet) -> Unit,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var index by rememberSaveable { mutableIntStateOf(-1) }
    var auto by rememberSaveable { mutableStateOf(false) }
    // true = la tele respondió y falta confirmar cuál de los dos últimos fue.
    var confirming by rememberSaveable { mutableStateOf(false) }

    fun sendAt(i: Int) {
        val set = sets.getOrNull(i) ?: return
        scope.launch { onSend(set.power, set.protocol) }
    }

    fun next() {
        if (index < sets.size - 1) {
            index++
            sendAt(index)
        } else {
            auto = false
        }
    }

    LaunchedEffect(auto) {
        while (auto) {
            next()
            delay(AUTO_STEP_MS)
        }
    }

    val current = sets.getOrNull(index)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Header(title = "Búsqueda automática", onBack = onBack)
        Text(
            "Tele: $tvName. Deja la tele ENCENDIDA y apunta el celular hacia ella. La app va a probar " +
                "el botón de encendido de ${sets.size} controles de distintas marcas (AOC y Philips primero). " +
                "Cuando la tele se APAGUE, toca «¡Se apagó!».",
            fontSize = 15.sp,
            lineHeight = 21.sp,
            color = MaterialTheme.colorScheme.onBackground,
        )

        LinearProgressIndicator(
            progress = { if (sets.isEmpty()) 0f else (index + 1).coerceAtLeast(0) / sets.size.toFloat() },
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            when {
                current != null -> "Control ${index + 1} de ${sets.size}: ${current.brand} · ${current.model}"
                else -> "Todavía no se prueba ninguno."
            },
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
        )

        if (confirming && current != null) {
            Text(
                "Confirmemos. Vuelve a encender la tele con su control original y toca «Probar este». " +
                    "Si se apaga otra vez, es este. Si no, prueba «El anterior».",
                fontSize = 15.sp,
                lineHeight = 21.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { sendAt(index) },
                    modifier = Modifier.weight(1f).heightIn(min = 52.dp),
                ) { Text("Probar este") }
                OutlinedButton(
                    enabled = index > 0,
                    onClick = {
                        index--
                        sendAt(index)
                    },
                    modifier = Modifier.weight(1f).heightIn(min = 52.dp),
                ) { Text("El anterior") }
            }
            Button(
                onClick = { onFound(current) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            ) { Text("¡Sí, es este! Guardar") }
            OutlinedButton(
                onClick = { confirming = false },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            ) { Text("No era, seguir buscando") }
        } else {
            Button(
                onClick = { auto = !auto },
                enabled = index < sets.size - 1 || auto,
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            ) { Text(if (auto) "Pausar" else if (index < 0) "Empezar búsqueda" else "Seguir buscando") }
            Button(
                onClick = {
                    auto = false
                    confirming = true
                },
                enabled = current != null,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            ) { Text("¡Se apagó!") }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    enabled = !auto && index > 0,
                    onClick = {
                        index--
                        sendAt(index)
                    },
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                ) { Text("Anterior") }
                OutlinedButton(
                    enabled = !auto && index < sets.size - 1,
                    onClick = { next() },
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                ) { Text("Siguiente") }
            }
            if (index >= sets.size - 1 && sets.isNotEmpty() && !auto) {
                Text(
                    "Se probaron todos. Si la tele nunca se apagó, su control no está en la base: " +
                        "habrá que leer el control original con un control universal que aprenda códigos.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
