package com.controllg.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.controllg.app.APP_KEYS
import com.controllg.app.NUMBER_KEYS
import com.controllg.app.RemoteKey
import com.controllg.app.TvConfig

@Composable
fun RemoteScreen(
    codes: Map<RemoteKey, Long?>,
    hiddenApps: Set<RemoteKey>,
    onSend: suspend (RemoteKey) -> Unit,
    onOpenSettings: () -> Unit,
    onShare: () -> Unit,
    tvs: List<TvConfig>,
    selectedTvId: String,
    onSelectTv: (String) -> Unit,
) {
    var showNumbers by rememberSaveable { mutableStateOf(false) }
    val press: (RemoteKey) -> (suspend () -> Unit) = { key -> suspend { onSend(key) } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Header(title = "Control Casa") {
            IconButton(onClick = onShare) {
                Icon(
                    Icons.Filled.Share,
                    contentDescription = "Compartir la app",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onOpenSettings) {
                Icon(
                    Icons.Filled.Settings,
                    contentDescription = "Configuración de la app",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (tvs.size > 1) TvSelector(tvs, selectedTvId, onSelectTv)

        // Encendido y entrada
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PadButton(
                description = RemoteKey.POWER.label,
                onSend = press(RemoteKey.POWER),
                modifier = Modifier.size(84.dp),
                color = PowerRed,
                contentColor = Color.White,
            ) {
                PowerIcon(Modifier.size(36.dp), Color.White)
            }
            if (RemoteKey.POWER_ON in codes) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    PillButton("Encender", press(RemoteKey.POWER_ON), Modifier.fillMaxWidth())
                    PillButton("Apagar", press(RemoteKey.POWER_OFF), Modifier.fillMaxWidth())
                }
            } else {
                Spacer(Modifier.weight(1f))
            }
            PillButton(
                text = "Entrada",
                onSend = press(RemoteKey.INPUT),
                modifier = Modifier
                    .width(120.dp)
                    .height(64.dp),
            )
        }

        // Volumen · cruceta · canal
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Rocker(
                title = "VOL",
                up = RemoteKey.VOL_UP,
                middle = RemoteKey.MUTE,
                down = RemoteKey.VOL_DOWN,
                press = press,
            )
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                DPad(
                    press = press,
                    modifier = Modifier
                        .widthIn(max = 250.dp)
                        .fillMaxWidth()
                        .aspectRatio(1f),
                )
            }
            Rocker(
                title = "CANAL",
                up = RemoteKey.CH_UP,
                middle = null,
                down = RemoteKey.CH_DOWN,
                press = press,
            )
        }

        // Atrás · Home · Salir
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            PillButton("Atrás", press(RemoteKey.BACK), Modifier.weight(1f))
            PadButton(
                description = RemoteKey.HOME.label,
                onSend = press(RemoteKey.HOME),
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                shape = RoundedCornerShape(18.dp),
            ) {
                Icon(Icons.Filled.Home, contentDescription = null, modifier = Modifier.size(28.dp))
            }
            PillButton("Salir", press(RemoteKey.EXIT), Modifier.weight(1f))
        }

        // Accesos rápidos: apps visibles + configuración de la tele
        val shortcuts = APP_KEYS.filter { it in codes && it !in hiddenApps } + RemoteKey.SETTINGS
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            shortcuts.chunked(3).forEach { rowKeys ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    rowKeys.forEach { key ->
                        PillButton(
                            text = if (key == RemoteKey.SETTINGS) "Ajustes TV" else key.label,
                            onSend = press(key),
                            modifier = Modifier.weight(1f),
                            color = shortcutColor(key),
                            contentColor = Color.White,
                        )
                    }
                    repeat(3 - rowKeys.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }

        // Teclado numérico desplegable
        PadButton(
            description = "Teclado numérico",
            onSend = { showNumbers = !showNumbers },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Teclado numérico", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Icon(
                    if (showNumbers) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                )
            }
        }
        AnimatedVisibility(visible = showNumbers) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                NUMBER_KEYS.take(9).chunked(3).forEach { rowKeys ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        rowKeys.forEach { key -> NumberButton(key, press, Modifier.weight(1f)) }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Spacer(Modifier.weight(1f))
                    NumberButton(RemoteKey.NUM_0, press, Modifier.weight(1f))
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

private fun shortcutColor(key: RemoteKey): Color = when (key) {
    RemoteKey.NETFLIX -> Color(0xFFB1060F)
    RemoteKey.PRIME -> Color(0xFF146EB4)
    RemoteKey.DISNEY -> Color(0xFF1F3A93)
    RemoteKey.YOUTUBE -> Color(0xFFC4302B)
    RemoteKey.MAX -> Color(0xFF4B2BB3)
    RemoteKey.SPOTIFY -> Color(0xFF168D40)
    else -> Color(0xFF3A3E47)
}

/** Elegir qué tele se maneja: un botón por tele, la elegida resaltada. */
@Composable
private fun TvSelector(
    tvs: List<TvConfig>,
    selectedTvId: String,
    onSelectTv: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        tvs.forEach { tv ->
            val selected = tv.id == selectedTvId
            Box(
                modifier = Modifier
                    .widthIn(min = 140.dp)
                    .height(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                    )
                    .selectable(selected = selected, role = Role.Tab, onClick = { onSelectTv(tv.id) })
                    .padding(horizontal = 18.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = tv.name,
                    fontSize = 16.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun NumberButton(
    key: RemoteKey,
    press: (RemoteKey) -> (suspend () -> Unit),
    modifier: Modifier = Modifier,
) {
    PadButton(
        description = key.label,
        onSend = press(key),
        modifier = modifier.height(60.dp),
        shape = RoundedCornerShape(18.dp),
    ) {
        Text(key.label, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** Columna vertical de Volumen o Canal. + y − se repiten mientras se mantienen presionados. */
@Composable
private fun Rocker(
    title: String,
    up: RemoteKey,
    middle: RemoteKey?,
    down: RemoteKey,
    press: (RemoteKey) -> (suspend () -> Unit),
) {
    Column(
        modifier = Modifier
            .width(64.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(MaterialTheme.colorScheme.surface),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        PadButton(
            description = up.label,
            onSend = press(up),
            modifier = Modifier
                .fillMaxWidth()
                .height(76.dp),
            shape = RoundedCornerShape(32.dp),
            repeat = true,
        ) {
            Text("+", fontSize = 32.sp, fontWeight = FontWeight.Medium)
        }
        if (middle != null) {
            PadButton(
                description = middle.label,
                onSend = press(middle),
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .size(52.dp),
                color = MaterialTheme.colorScheme.background,
            ) {
                Text("MUDO", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        } else {
            Spacer(Modifier.height(18.dp))
        }
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 6.dp),
        )
        if (middle == null) Spacer(Modifier.height(18.dp))
        PadButton(
            description = down.label,
            onSend = press(down),
            modifier = Modifier
                .fillMaxWidth()
                .height(76.dp),
            shape = RoundedCornerShape(32.dp),
            repeat = true,
        ) {
            Text("−", fontSize = 32.sp, fontWeight = FontWeight.Medium)
        }
    }
}

/** Cruceta circular con OK al medio. */
@Composable
private fun DPad(
    press: (RemoteKey) -> (suspend () -> Unit),
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface),
    ) {
        DPadArrow(RemoteKey.UP, Icons.Filled.KeyboardArrowUp, press, Modifier.align(Alignment.TopCenter))
        DPadArrow(RemoteKey.DOWN, Icons.Filled.KeyboardArrowDown, press, Modifier.align(Alignment.BottomCenter))
        DPadArrow(RemoteKey.LEFT, Icons.AutoMirrored.Filled.KeyboardArrowLeft, press, Modifier.align(Alignment.CenterStart))
        DPadArrow(RemoteKey.RIGHT, Icons.AutoMirrored.Filled.KeyboardArrowRight, press, Modifier.align(Alignment.CenterEnd))
        PadButton(
            description = RemoteKey.OK.label,
            onSend = press(RemoteKey.OK),
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxSize(0.36f),
            color = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ) {
            Text("OK", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun DPadArrow(
    key: RemoteKey,
    icon: ImageVector,
    press: (RemoteKey) -> (suspend () -> Unit),
    modifier: Modifier = Modifier,
) {
    PadButton(
        description = key.label,
        onSend = press(key),
        modifier = modifier
            .fillMaxSize(0.32f)
            .padding(4.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.fillMaxSize(0.7f))
    }
}

/** Símbolo de encendido dibujado a mano (no viene en los íconos básicos). */
@Composable
fun PowerIcon(modifier: Modifier = Modifier, color: Color = Color.White) {
    Canvas(modifier) {
        val stroke = size.minDimension * 0.11f
        val inset = stroke
        drawArc(
            color = color,
            startAngle = -60f,
            sweepAngle = 300f,
            useCenter = false,
            topLeft = Offset(inset, inset),
            size = Size(size.width - inset * 2, size.height - inset * 2),
            style = Stroke(width = stroke, cap = StrokeCap.Round),
        )
        drawLine(
            color = color,
            start = Offset(size.width / 2, stroke / 2),
            end = Offset(size.width / 2, size.height / 2),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}
