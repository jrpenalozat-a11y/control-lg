package com.controllg.app

import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.controllg.app.ui.ControlLgTheme
import com.controllg.app.ui.EditorScreen
import com.controllg.app.ui.PowerIcon
import com.controllg.app.ui.RemoteScreen
import com.controllg.app.ui.SettingsScreen
import com.controllg.app.ui.WizardScreen
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
        )
        val ir = IrSender(applicationContext)
        val repository = SettingsRepository(applicationContext)
        setContent {
            ControlLgTheme {
                ControlLgApp(ir, repository)
            }
        }
    }
}

private enum class Screen { REMOTE, SETTINGS, EDITOR, WIZARD }

@Composable
private fun ControlLgApp(ir: IrSender, repository: SettingsRepository) {
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val loaded by repository.settings.collectAsState(initial = null)
    var chosenScreen by rememberSaveable { mutableStateOf<Screen?>(null) }

    val context = LocalContext.current
    val view = LocalView.current
    val keepScreenOn = loaded?.keepScreenOn == true
    DisposableEffect(view, keepScreenOn) {
        view.keepScreenOn = keepScreenOn
        onDispose { view.keepScreenOn = false }
    }

    /** Muestra un aviso. Con [replace] = false no apila avisos repetidos (botón mantenido). */
    fun notify(message: String, replace: Boolean = true) {
        if (!replace && snackbar.currentSnackbarData != null) return
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            snackbar.showSnackbar(message)
        }
    }

    suspend fun sendCode(code: Long?) {
        if (code == null) {
            notify("Este botón no tiene código. Agrégalo en Configuración → Editor de códigos.", replace = false)
            return
        }
        ir.send(code).onFailure { e ->
            notify(
                "No se pudo enviar la señal infrarroja: ${e.message ?: e.javaClass.simpleName}",
                replace = false,
            )
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
                .imePadding(),
        ) {
            val settings = loaded
            if (!ir.hasEmitter) {
                NoEmitterScreen()
            } else if (settings != null) {
                val codes = remember(settings) { settings.effectiveCodes() }
                val screen = chosenScreen ?: if (settings.wizardDone) Screen.REMOTE else Screen.WIZARD

                BackHandler(enabled = screen != Screen.REMOTE && settings.wizardDone) {
                    chosenScreen = if (screen == Screen.SETTINGS) Screen.REMOTE else Screen.SETTINGS
                }

                when (screen) {
                    Screen.REMOTE -> RemoteScreen(
                        codes = codes,
                        hiddenApps = settings.hiddenApps,
                        onSend = { key -> sendCode(codes[key]) },
                        onOpenSettings = { chosenScreen = Screen.SETTINGS },
                        onShare = {
                            shareApp(context).onFailure { e ->
                                notify("No se pudo compartir la app: ${e.message ?: e.javaClass.simpleName}")
                            }
                        },
                    )

                    Screen.SETTINGS -> SettingsScreen(
                        settings = settings,
                        repository = repository,
                        notify = { notify(it) },
                        onBack = { chosenScreen = Screen.REMOTE },
                        onOpenEditor = { chosenScreen = Screen.EDITOR },
                        onOpenWizard = { chosenScreen = Screen.WIZARD },
                    )

                    Screen.EDITOR -> EditorScreen(
                        settings = settings,
                        codes = codes,
                        onSetCode = { key, code ->
                            scope.launch { repository.setCode(settings.profileId, key, code) }
                        },
                        onReset = {
                            scope.launch {
                                repository.resetProfile(settings.profileId)
                                notify("Valores de fábrica restaurados")
                            }
                        },
                        onTest = { code -> sendCode(code) },
                        onBack = { chosenScreen = Screen.SETTINGS },
                    )

                    Screen.WIZARD -> WizardScreen(
                        firstRun = !settings.wizardDone,
                        onSend = { code -> sendCode(code) },
                        onFinish = { profileId ->
                            scope.launch {
                                if (profileId != null) repository.setProfile(profileId)
                                repository.setWizardDone(true)
                                chosenScreen = Screen.REMOTE
                                if (profileId != null) {
                                    notify("Listo: quedó guardado «${profileById(profileId).name}»")
                                }
                            }
                        },
                        onCancel = {
                            scope.launch {
                                val wasFirstRun = !settings.wizardDone
                                repository.setWizardDone(true)
                                chosenScreen = if (wasFirstRun) Screen.REMOTE else Screen.SETTINGS
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun NoEmitterScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        PowerIcon(Modifier.size(72.dp), MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = "Este celular no tiene emisor infrarrojo",
            fontSize = 24.sp,
            lineHeight = 30.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = "Control LG necesita el emisor infrarrojo (IR) del teléfono para manejar la tele. " +
                "Lo traen, por ejemplo, muchos Xiaomi, Redmi, POCO y Huawei. " +
                "Este teléfono no lo tiene, así que la app no puede enviar señales desde aquí.",
            fontSize = 16.sp,
            lineHeight = 22.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
