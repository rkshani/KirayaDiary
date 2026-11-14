package com.kiraya.diary.ui.lock

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.CompositionLocalProvider
import com.kiraya.diary.R

const val PIN_MIN = 4
const val PIN_MAX = 6

/**
 * Number pad for entering a PIN. Calls [onComplete] when the user taps OK.
 * Digits are always laid out left-to-right, also in Urdu.
 */
@Composable
fun PinPad(
    title: String,
    error: String?,
    onComplete: (String) -> Unit,
    modifier: Modifier = Modifier,
    extra: (@Composable () -> Unit)? = null,
    clearKey: Any = Unit,
) {
    var pin by remember(clearKey) { mutableStateOf("") }
    Column(
        modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(
            painterResource(R.drawable.ic_lock),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(48.dp),
        )
        Text(title, style = MaterialTheme.typography.titleLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            repeat(PIN_MAX) { i ->
                Box(
                    Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                i < pin.length -> MaterialTheme.colorScheme.primary
                                i < PIN_MIN -> MaterialTheme.colorScheme.outline
                                else -> MaterialTheme.colorScheme.outlineVariant
                            },
                        ),
                )
            }
        }
        Text(error.orEmpty(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf("⌫", "0", "OK")).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        row.forEach { key ->
                            FilledTonalButton(
                                onClick = {
                                    when (key) {
                                        "⌫" -> pin = pin.dropLast(1)
                                        "OK" -> if (pin.length >= PIN_MIN) {
                                            onComplete(pin)
                                            pin = ""
                                        }
                                        else -> if (pin.length < PIN_MAX) pin += key
                                    }
                                },
                                enabled = key != "OK" || pin.length >= PIN_MIN,
                                modifier = Modifier.size(72.dp),
                                shape = CircleShape,
                            ) {
                                Text(if (key == "OK") stringResource(R.string.action_ok) else key, style = MaterialTheme.typography.titleLarge)
                            }
                        }
                    }
                }
            }
        }
        extra?.invoke()
    }
}

/** Full-screen lock shown before the app content. */
@Composable
fun LockScreen(
    biometricEnabled: Boolean,
    checkPin: (String) -> Boolean,
    onUnlocked: () -> Unit,
) {
    val context = LocalContext.current
    var error by remember { mutableStateOf<String?>(null) }
    var attempts by remember { mutableStateOf(0) }
    val wrong = stringResource(R.string.lock_wrong_pin)
    val promptTitle = stringResource(R.string.lock_biometric_title)
    val usePin = stringResource(R.string.lock_use_pin)
    val canBio = biometricEnabled && Biometrics.isAvailable(context)

    fun askBiometric() = Biometrics.prompt(context, promptTitle, usePin, onUnlocked)

    LaunchedEffect(canBio) { if (canBio) askBiometric() }

    Surface(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().systemBarsPadding(), contentAlignment = Alignment.Center) {
            PinPad(
                title = stringResource(R.string.lock_enter_pin),
                error = error,
                clearKey = attempts,
                onComplete = { pin ->
                    if (checkPin(pin)) {
                        onUnlocked()
                    } else {
                        attempts++
                        error = wrong
                    }
                },
                extra = if (canBio) {
                    {
                        Spacer(Modifier.height(4.dp))
                        TextButton(onClick = ::askBiometric) { Text(stringResource(R.string.lock_use_biometric)) }
                    }
                } else {
                    null
                },
            )
        }
    }
}
