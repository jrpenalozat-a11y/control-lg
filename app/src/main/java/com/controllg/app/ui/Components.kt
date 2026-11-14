package com.controllg.app.ui

import android.os.SystemClock
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Cada cuánto se repite el envío mientras se mantiene presionado Volumen o Canal. */
private const val REPEAT_MS = 110L

/**
 * Botón del control. Vibra en cada pulsación. Con [repeat] envía al tocar y sigue
 * enviando cada ~110 ms mientras el dedo siga apoyado.
 */
@Composable
fun PadButton(
    description: String,
    onSend: suspend () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = CircleShape,
    color: Color = MaterialTheme.colorScheme.surfaceVariant,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    repeat: Boolean = false,
    content: @Composable BoxScope.() -> Unit,
) {
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val currentSend by rememberUpdatedState(onSend)
    var pressed by remember { mutableStateOf(false) }

    val gesture = if (repeat) {
        Modifier
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                        pressed = true
                        val job = scope.launch {
                            while (isActive) {
                                val start = SystemClock.elapsedRealtime()
                                currentSend()
                                val spent = SystemClock.elapsedRealtime() - start
                                delay((REPEAT_MS - spent).coerceAtLeast(10L))
                            }
                        }
                        try {
                            tryAwaitRelease()
                        } finally {
                            job.cancel()
                            pressed = false
                        }
                    },
                )
            }
            .semantics {
                role = Role.Button
                contentDescription = description
                onClick {
                    scope.launch { currentSend() }
                    true
                }
            }
    } else {
        Modifier
            .clickable(role = Role.Button, onClickLabel = description) {
                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                scope.launch { currentSend() }
            }
            .semantics { contentDescription = description }
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(if (pressed) lerp(color, Color.White, 0.25f) else color)
            .then(gesture),
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            content()
        }
    }
}

/** Botón rectangular con texto. */
@Composable
fun PillButton(
    text: String,
    onSend: suspend () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.surfaceVariant,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    PadButton(
        description = text,
        onSend = onSend,
        modifier = modifier.heightIn(min = 56.dp),
        shape = RoundedCornerShape(18.dp),
        color = color,
        contentColor = contentColor,
    ) {
        Text(
            text = text,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 8.dp),
        )
    }
}

/** Encabezado de cada pantalla. */
@Composable
fun Header(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
            }
            Spacer(Modifier.width(4.dp))
        }
        Text(
            text = title,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        actions()
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(
        text = text.uppercase(),
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp),
    )
}
