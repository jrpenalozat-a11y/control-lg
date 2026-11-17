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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.controllg.app.IrProtocol
import com.controllg.app.WIZARD_PROFILES
import com.controllg.app.RemoteKey
import kotlinx.coroutines.launch

/**
 * Asistente «Probar mi tele»: envía el código de encendido de cada perfil y pregunta
 * si la tele respondió. Guarda el primero que funcione.
 */
@Composable
fun WizardScreen(
    firstRun: Boolean,
    onSend: suspend (Long?, IrProtocol) -> Unit,
    /** Perfil que funcionó, o null para seguir con el que ya estaba. */
    onFinish: (String?) -> Unit,
    onCancel: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var index by rememberSaveable { mutableIntStateOf(0) }
    var sent by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Header(title = "Probar mi tele", onBack = if (firstRun) null else onCancel)

        if (index < WIZARD_PROFILES.size) {
            val profile = WIZARD_PROFILES[index]
            Text(
                "Paso ${index + 1} de ${WIZARD_PROFILES.size}",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                profile.name,
                fontSize = 22.sp,
                lineHeight = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                "Apunta la parte de arriba del celular hacia la tele, a unos 2 o 3 metros, " +
                    "y toca el botón para enviar la señal.",
                fontSize = 16.sp,
                lineHeight = 22.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            profile.testKeys.forEach { key ->
                Button(
                    onClick = {
                        sent = true
                        scope.launch { onSend(profile.codes[key], profile.protocol) }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 64.dp),
                ) {
                    Text(
                        text = when (key) {
                            RemoteKey.POWER_ON -> "Enviar «Encender»"
                            RemoteKey.POWER_OFF -> "Enviar «Apagar»"
                            else -> "Enviar «Encender/Apagar»"
                        },
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            if (sent) {
                Text(
                    "¿Respondió la tele?",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 12.dp),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = { onFinish(profile.id) },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 60.dp),
                    ) {
                        Text("Sí", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = {
                            index++
                            sent = false
                        },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 60.dp),
                    ) {
                        Text("No", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            TextButton(onClick = onCancel) {
                Text(if (firstRun) "Omitir por ahora" else "Cancelar", color = Color(0xFFB4B9C2))
            }
        } else {
            Text(
                "La tele no respondió con ningún perfil",
                fontSize = 22.sp,
                lineHeight = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                "Antes de rendirte, revisa esto:\n\n" +
                    "• Apunta el borde superior del celular directo al frente de la tele.\n" +
                    "• Acércate a 1 o 2 metros y quita la funda si tapa el emisor.\n" +
                    "• Comprueba que la tele esté enchufada (luz roja encendida).\n\n" +
                    "Si igual no responde, puedes seguir con el perfil estándar y corregir cada botón " +
                    "en Configuración → Editor de códigos.",
                fontSize = 16.sp,
                lineHeight = 22.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                onClick = {
                    index = 0
                    sent = false
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp),
            ) {
                Text("Probar de nuevo")
            }
            OutlinedButton(
                onClick = { onFinish(null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp),
            ) {
                Text("Seguir sin cambiar el perfil")
            }
        }
    }
}
