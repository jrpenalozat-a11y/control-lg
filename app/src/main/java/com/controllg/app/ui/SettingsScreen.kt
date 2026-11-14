package com.controllg.app.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.controllg.app.APP_KEYS
import com.controllg.app.AppSettings
import com.controllg.app.PROFILES
import com.controllg.app.SettingsRepository
import com.controllg.app.exportConfigJson
import com.controllg.app.parseConfigJson
import com.controllg.app.profileById
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SettingsScreen(
    settings: AppSettings,
    repository: SettingsRepository,
    notify: (String) -> Unit,
    onBack: () -> Unit,
    onOpenEditor: () -> Unit,
    onOpenWizard: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentSettings by rememberUpdatedState(settings)

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val json = exportConfigJson(currentSettings)
                    withContext(Dispatchers.IO) {
                        val stream = context.contentResolver.openOutputStream(uri)
                            ?: throw IllegalStateException("no se pudo abrir el archivo")
                        stream.use { it.write(json.toByteArray(Charsets.UTF_8)) }
                    }
                    notify("Configuración exportada")
                } catch (e: Exception) {
                    notify("No se pudo exportar: ${e.message ?: e.javaClass.simpleName}")
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val text = withContext(Dispatchers.IO) {
                        val stream = context.contentResolver.openInputStream(uri)
                            ?: throw IllegalStateException("no se pudo abrir el archivo")
                        stream.use { it.readBytes().toString(Charsets.UTF_8) }
                    }
                    val config = parseConfigJson(text)
                    repository.applyImport(config)
                    notify("Configuración importada: ${profileById(config.profileId).name}")
                } catch (e: Exception) {
                    notify("No se pudo importar: ${e.message ?: e.javaClass.simpleName}")
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Header(title = "Configuración", onBack = onBack)

        SectionTitle("Perfil de códigos")
        PROFILES.forEach { profile ->
            val selected = profile.id == settings.profileId
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .selectable(
                        selected = selected,
                        role = Role.RadioButton,
                        onClick = { scope.launch { repository.setProfile(profile.id) } },
                    )
                    .heightIn(min = 60.dp)
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = selected, onClick = null, modifier = Modifier.padding(horizontal = 12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        profile.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        profile.description,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        Button(onClick = onOpenWizard, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
            Text("Probar mi tele")
        }
        OutlinedButton(onClick = onOpenEditor, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
            Text("Editor de códigos")
        }

        SectionTitle("Botones de apps")
        Text(
            "No todos los modelos LG traen estas apps. Apaga las que tu tele no tenga.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        val codes = settings.effectiveCodes()
        APP_KEYS.forEach { key ->
            SwitchRow(
                title = key.label,
                subtitle = if (codes[key] == null) "Sin código: agrégalo en el Editor de códigos" else null,
                checked = key !in settings.hiddenApps,
                onChange = { visible -> scope.launch { repository.setAppHidden(key, !visible) } },
            )
        }

        SectionTitle("Pantalla")
        SwitchRow(
            title = "Mantener la pantalla encendida",
            subtitle = "Mientras la app esté abierta",
            checked = settings.keepScreenOn,
            onChange = { on -> scope.launch { repository.setKeepScreenOn(on) } },
        )

        SectionTitle("Compartir configuración")
        Text(
            "Guarda tus códigos en un archivo JSON para pasarlos a otro celular.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick = {
                    try {
                        exportLauncher.launch("control-lg-codigos.json")
                    } catch (e: Exception) {
                        notify("Este celular no tiene una app para guardar archivos")
                    }
                },
                modifier = Modifier.weight(1f).heightIn(min = 52.dp),
            ) {
                Text("Exportar")
            }
            OutlinedButton(
                onClick = {
                    try {
                        importLauncher.launch(arrayOf("*/*"))
                    } catch (e: Exception) {
                        notify("Este celular no tiene una app para elegir archivos")
                    }
                },
                modifier = Modifier.weight(1f).heightIn(min = 52.dp),
            ) {
                Text("Importar")
            }
        }
    }
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String?,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .toggleable(value = checked, role = Role.Switch, onValueChange = onChange)
            .heightIn(min = 56.dp)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp, color = MaterialTheme.colorScheme.onBackground)
            if (subtitle != null) {
                Text(subtitle, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Switch(checked = checked, onCheckedChange = null)
    }
}
