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
import androidx.compose.runtime.LaunchedEffect
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
import com.controllg.app.IrProtocol
import com.controllg.app.brandOf
import com.controllg.app.candidateCode
import com.controllg.app.commandCount
import com.controllg.app.RemoteKey
import com.controllg.app.formatHexCode
import com.controllg.app.parseHexCode
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

/** Ver y cambiar el código hexadecimal de cada botón, sin recompilar. */
@Composable
fun EditorScreen(
    settings: AppSettings,
    codes: Map<RemoteKey, Long?>,
    /** Botón cuyo Buscador se abre de inmediato (al tocar en el control un botón sin código). */
    initialSearch: RemoteKey? = null,
    onSearchClosed: () -> Unit = {},
    onSetCode: (RemoteKey, Long?) -> Unit,
    onReset: () -> Unit,
    onTest: suspend (Long?) -> Unit,
    onBack: () -> Unit,
) {
    val profile = settings.profile
    var confirmReset by remember { mutableStateOf(false) }
    // Cambia al restaurar, para que los campos vuelvan a mostrar los valores de fábrica.
    var resetCount by remember { mutableIntStateOf(0) }
    var searching by remember { mutableStateOf(initialSearch) }

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
            "Tele: ${settings.tv.name} · Perfil: ${profile.name}. " +
                "Cada código son 8 dígitos hexadecimales (por ejemplo 0x20DF10EF). " +
                "Los cambios se guardan solos. Apunta a la tele y toca «Probar». " +
                "Si no sabes el código, toca «Buscar» y prueba uno por uno hasta que la tele responda.",
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
                    onSearch = { searching = remoteKey },
                )
            }
        }
    }

    searching?.let { target ->
        CodeFinderDialog(
            target = target,
            codes = codes,
            protocol = settings.protocol,
            onTest = onTest,
            onSave = { code ->
                onSetCode(target, code)
                resetCount++
            },
            onDismiss = {
                searching = null
                onSearchClosed()
            },
        )
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
    onSearch: () -> Unit,
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
        Column(
            modifier = Modifier.padding(top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Button(
                onClick = { scope.launch { onTest(current) } },
                enabled = current != null,
                modifier = Modifier.heightIn(min = 48.dp),
            ) {
                Text("Probar")
            }
            OutlinedButton(onClick = onSearch, modifier = Modifier.heightIn(min = 40.dp)) {
                Text("Buscar")
            }
        }
    }
}

/**
 * Buscador de código: envía, uno por uno, los 256 códigos posibles de una marca
 * (los 4 primeros dígitos fijos) hasta que la tele hace lo que se busca.
 */
@Composable
private fun CodeFinderDialog(
    target: RemoteKey,
    codes: Map<RemoteKey, Long?>,
    protocol: IrProtocol,
    onTest: suspend (Long?) -> Unit,
    onSave: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val known = remember(codes) { codes.values.filterNotNull().toSet() }
    val defaultPrefix = remember(codes) {
        val base = codes[RemoteKey.POWER] ?: codes.values.firstOrNull { it != null }
        val brand = if (base != null) brandOf(protocol, base) else if (protocol == IrProtocol.NEC) 0x20DFL else 0L
        String.format(Locale.ROOT, "%04X", brand)
    }
    var prefixText by remember { mutableStateOf(defaultPrefix) }
    val prefix = prefixText.takeIf { it.length == 4 }?.toLongOrNull(16)
    // Posición dentro de los 256 comandos; -1 = todavía no se envía ninguno.
    var index by remember(prefix) { mutableIntStateOf(-1) }

    val total = commandCount(protocol)
    fun candidate(i: Int): Long = candidateCode(protocol, prefix ?: 0L, i)

    /** Siguiente comando que no sea de otro botón (así no se apaga la tele por error). */
    fun next(from: Int): Int {
        var i = from + 1
        while (i < total && candidate(i) in known) i++
        return i
    }

    fun previous(from: Int): Int {
        var i = from - 1
        while (i > 0 && candidate(i) in known) i--
        return i.coerceAtLeast(0)
    }

    fun sendNext() {
        index = next(index)
        if (index < total) scope.launch { onTest(candidate(index)) }
    }

    // Modo automático: un código cada 3 segundos hasta que se pause o se acaben.
    var auto by remember(prefix) { mutableStateOf(false) }
    LaunchedEffect(auto) {
        while (auto) {
            sendNext()
            if (index >= total) auto = false
            delay(3000)
        }
    }

    val currentCode = if (index in 0 until total) candidate(index) else null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Buscar código: ${target.label}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Apunta a la tele y toca «Automático»: la app prueba un código cada 3 segundos. " +
                        "Cuando la tele haga lo de «${target.label}», toca «Pausar» y luego «Repetir» " +
                        "(o «Anterior») para confirmar cuál fue, y «¡Funcionó! Guardar». " +
                        "Si un código apaga la tele o abre un menú, no pasa nada: sigue.",
                    fontSize = 14.sp,
                    lineHeight = 19.sp,
                )
                OutlinedTextField(
                    value = prefixText,
                    onValueChange = { input ->
                        prefixText = input.filter { it.isLetterOrDigit() }.take(4).uppercase()
                    },
                    label = { Text("Marca (4 primeros dígitos)") },
                    supportingText = { Text(if (prefix == null) "Deben ser 4 dígitos hexadecimales" else "Viene con la marca de tu tele: no lo cambies") },
                    isError = prefix == null,
                    singleLine = true,
                    textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 16.sp),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters,
                        keyboardType = KeyboardType.Ascii,
                    ),
                )
                Text(
                    when {
                        currentCode != null -> "Prueba ${index + 1} de $total · ${formatHexCode(currentCode)}"
                        index >= total -> "Se probaron los $total códigos. Prueba otra marca o revisa la tele."
                        else -> "Todavía no se envía ningún código."
                    },
                    fontFamily = FontFamily.Monospace,
                    fontSize = 15.sp,
                )
                Button(
                    enabled = prefix != null && (index < total - 1 || auto),
                    onClick = { auto = !auto },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                ) { Text(if (auto) "Pausar" else "Automático") }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        enabled = !auto && index > 0,
                        onClick = {
                            index = previous(index)
                            scope.launch { onTest(candidate(index)) }
                        },
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                    ) { Text("Anterior", maxLines = 1) }
                    OutlinedButton(
                        enabled = !auto && currentCode != null,
                        onClick = { scope.launch { onTest(currentCode) } },
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                    ) { Text("Repetir", maxLines = 1) }
                    OutlinedButton(
                        enabled = !auto && prefix != null && index < total,
                        onClick = { sendNext() },
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                    ) { Text("Siguiente", maxLines = 1) }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = currentCode != null,
                onClick = {
                    auto = false
                    currentCode?.let(onSave)
                    onDismiss()
                },
            ) { Text("¡Funcionó! Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } },
    )
}
