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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
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
import com.controllg.app.ui.AutoSearchScreen
import com.controllg.app.ui.ControlLgTheme
import com.controllg.app.ui.EditorScreen
import com.controllg.app.ui.ExploreScreen
import com.controllg.app.ui.PowerIcon
import com.controllg.app.ui.RemoteScreen
import com.controllg.app.ui.SettingsScreen
import com.controllg.app.ui.WizardScreen
import kotlinx.coroutines.delay
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

private enum class Screen { REMOTE, SETTINGS, EDITOR, WIZARD, AUTOSEARCH, EXPLORE }

@Composable
private fun ControlLgApp(ir: IrSender, repository: SettingsRepository) {
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val loaded by repository.settings.collectAsState(initial = null)
    var chosenScreen by rememberSaveable { mutableStateOf<Screen?>(null) }
    // Botón sin código tocado en el control: se abre el Buscador para él y se vuelve al control.
    var searchKey by rememberSaveable { mutableStateOf<RemoteKey?>(null) }

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

    suspend fun sendCode(code: Long?, protocol: IrProtocol = loaded?.protocol ?: IrProtocol.NEC) {
        if (code == null) {
            notify("Este botón no tiene código. Agrégalo en Configuración → Editor de códigos.", replace = false)
            return
        }
        ir.send(code, protocol).onFailure { e ->
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
                NoEmitterScreen(
                    onShare = {
                        shareApp(context).onFailure { e ->
                            notify("No se pudo compartir la app: ${e.message ?: e.javaClass.simpleName}")
                        }
                    },
                )
            } else if (settings != null) {
                val codes = remember(settings) { settings.effectiveCodes() }
                val screen = chosenScreen ?: if (settings.wizardDone) Screen.REMOTE else Screen.WIZARD

                BackHandler(enabled = screen != Screen.REMOTE && settings.wizardDone) {
                    chosenScreen = when {
                        screen == Screen.SETTINGS -> Screen.REMOTE
                        screen == Screen.EDITOR && searchKey != null -> Screen.REMOTE
                        else -> Screen.SETTINGS
                    }
                    searchKey = null
                }

                when (screen) {
                    Screen.REMOTE -> RemoteScreen(
                        codes = codes,
                        hiddenApps = settings.hiddenApps,
                        onSend = { key ->
                            val code = codes[key]
                            val macro = settings.profile.macros[key]
                                ?.takeIf { settings.tv.protocolOverride == null }
                                ?.takeIf { steps -> steps.all { codes[it] != null } }
                            if (code == null && macro != null) {
                                notify("Abriendo ${key.label}…")
                                for (step in macro) {
                                    sendCode(codes[step])
                                    delay(if (step == RemoteKey.HOME) 3000 else 700)
                                }
                            } else if (code == null) {
                                searchKey = key
                                chosenScreen = Screen.EDITOR
                            } else {
                                sendCode(code)
                            }
                        },
                        onOpenSettings = { chosenScreen = Screen.SETTINGS },
                        onShare = {
                            shareApp(context).onFailure { e ->
                                notify("No se pudo compartir la app: ${e.message ?: e.javaClass.simpleName}")
                            }
                        },
                        tvs = settings.tvs,
                        selectedTvId = settings.tv.id,
                        onSelectTv = { id -> scope.launch { repository.selectTv(id) } },
                    )

                    Screen.SETTINGS -> SettingsScreen(
                        settings = settings,
                        repository = repository,
                        notify = { notify(it) },
                        onBack = { chosenScreen = Screen.REMOTE },
                        onOpenEditor = { chosenScreen = Screen.EDITOR },
                        onOpenWizard = { chosenScreen = Screen.WIZARD },
                        onOpenAutoSearch = { chosenScreen = Screen.AUTOSEARCH },
                        onOpenExplore = { chosenScreen = Screen.EXPLORE },
                    )

                    Screen.EXPLORE -> ExploreScreen(
                        tvName = settings.tv.name,
                        protocol = settings.protocol,
                        codes = codes,
                        onTest = { code -> sendCode(code) },
                        onSetCode = { key, code ->
                            scope.launch { repository.setCode(settings.profileId, key, code) }
                        },
                        onBack = { chosenScreen = Screen.SETTINGS },
                    )

                    Screen.AUTOSEARCH -> {
                        val sets = remember { runCatching { loadCodeSets(context) }.getOrDefault(emptyList()) }
                        AutoSearchScreen(
                            tvName = settings.tv.name,
                            sets = sets,
                            onSend = { code, protocol -> sendCode(code, protocol) },
                            onFound = { set ->
                                scope.launch {
                                    repository.applyCodeSet(set)
                                    chosenScreen = Screen.REMOTE
                                    notify("Listo: «${settings.tv.name}» usa el control ${set.brand} · ${set.model}")
                                }
                            },
                            onBack = { chosenScreen = Screen.SETTINGS },
                        )
                    }

                    Screen.EDITOR -> EditorScreen(
                        settings = settings,
                        codes = codes,
                        initialSearch = searchKey,
                        onSearchClosed = {
                            if (searchKey != null) {
                                searchKey = null
                                chosenScreen = Screen.REMOTE
                            }
                        },
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
                        onBack = {
                            chosenScreen = if (searchKey != null) Screen.REMOTE else Screen.SETTINGS
                            searchKey = null
                        },
                    )

                    Screen.WIZARD -> WizardScreen(
                        firstRun = !settings.wizardDone,
                        onSend = { code, protocol -> sendCode(code, protocol) },
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
private fun NoEmitterScreen(onShare: () -> Unit) {
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
            text = "Control Casa necesita el emisor infrarrojo (IR) del teléfono para manejar la tele. " +
                "Lo traen, por ejemplo, muchos Xiaomi, Redmi, POCO y Huawei. " +
                "Este teléfono no lo tiene, así que la app no puede enviar señales desde aquí.",
            fontSize = 16.sp,
            lineHeight = 22.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(onClick = onShare) {
            Icon(Icons.Filled.Share, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Compartir la app", fontSize = 16.sp)
        }
    }
}
