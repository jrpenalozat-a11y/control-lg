package com.controllg.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.controllg.app.AppSettings
import com.controllg.app.RemoteKey
import com.controllg.app.formatHexCode
import com.controllg.app.parseHexCode
import kotlinx.coroutines.launch

/** Ver y cambiar el código hexadecimal de cada botón, sin recompilar. */
@Composable
fun EditorScreen(
    settings: AppSettings,
    codes: Map<RemoteKey, Long?>,
    onSetCode: (RemoteKey, Long?) -> Unit,
    onReset: () -> Unit,
    onTest: suspend (Long?) -> Unit,
    onBack: () -> Unit,
) {
    val profile = settings.profile
    var confirmReset by remember { mutableStateOf(false) }
    // Cambia al restaurar, para que los campos vuelvan a mostrar los valores de fábrica.
    var resetCount by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Header(title = "Editor de códigos", onBack = onBack)
        Text(
            "Perfil: ${profile.name}. Cada código son 8 dígitos hexadecimales (por ejemplo 0x20DF10EF). " +
                "Los cambios se guardan solos. Apunta a la tele y toca «Probar».",
            fontSize = 13.sp,
            lineHeight = 18.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedButton(
            onClick = { confirmReset = true },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .heightIn(min = 48.dp),
        ) {
            Text("Restaurar valores de fábrica")
        }

        profile.codes.forEach { (remoteKey, factory) ->
            key(profile.id, remoteKey) {
                CodeRow(
                    remoteKey = remoteKey,
                    factory = factory,
                    saved = codes[remoteKey],
                    resetCount = resetCount,
                    onSetCode = onSetCode,
                    onTest = onTest,
                )
            }
        }
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("¿Restaurar valores de fábrica?") },
            text = { Text("Se borran los cambios que hiciste en los códigos del perfil «${profile.name}».") },
            confirmButton = {
                TextButton(onClick = {
                    confirmReset = false
                    resetCount++
                    onReset()
                }) { Text("Restaurar") }
            },
            dismissButton = {
                TextButton(onClick = { confirmReset = false }) { Text("Cancelar") }
            },
        )
    }
}

@Composable
private fun CodeRow(
    remoteKey: RemoteKey,
    factory: Long?,
    saved: Long?,
    resetCount: Int,
    onSetCode: (RemoteKey, Long?) -> Unit,
    onTest: suspend (Long?) -> Unit,
) {
    val scope = rememberCoroutineScope()
    // Se vuelve a llenar cuando cambia el código guardado o se restauran los de fábrica.
    var text by remember(saved, resetCount) { mutableStateOf(saved?.let(::formatHexCode) ?: "") }

    val typed = parseHexCode(text)
    val emptyAllowed = text.isBlank() && factory == null
    val invalid = typed == null && !emptyAllowed
    val current = if (invalid) saved else typed

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = { input ->
                val clean = input.filter { !it.isWhitespace() }.take(10)
                text = clean
                val code = parseHexCode(clean)
                if (code != null) {
                    onSetCode(remoteKey, code)
                } else if (clean.isEmpty() && factory == null) {
                    onSetCode(remoteKey, null)
                }
            },
            label = { Text(remoteKey.label) },
            singleLine = true,
            isError = invalid,
            textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 16.sp),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Characters,
                keyboardType = KeyboardType.Ascii,
            ),
            supportingText = {
                Text(
                    when {
                        invalid -> "Deben ser 8 dígitos hexadecimales (0–9, A–F)"
                        current == null -> "Sin código"
                        current != factory && factory != null -> "Modificado · fábrica ${formatHexCode(factory)}"
                        current != factory -> "Agregado por ti"
                        else -> "De fábrica"
                    },
                )
            },
            modifier = Modifier.weight(1f),
        )
        Button(
            onClick = { scope.launch { onTest(current) } },
            enabled = current != null,
            modifier = Modifier
                .padding(top = 8.dp)
                .heightIn(min = 56.dp),
        ) {
            Text("Probar")
        }
    }
}
