package com.kiraya.diary.ui.lock

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.kiraya.diary.R
import com.kiraya.diary.core.security.PinHasher
import com.kiraya.diary.data.security.AppLockManager
import com.kiraya.diary.data.settings.AppSettings
import com.kiraya.diary.data.settings.SettingsRepository
import com.kiraya.diary.ui.components.ConfirmDialog
import com.kiraya.diary.ui.components.KirayaTopBar
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppLockSettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val lockManager: AppLockManager,
) : ViewModel() {
    val settings: StateFlow<AppSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    fun savePin(pin: String) {
        viewModelScope.launch {
            val salt = PinHasher.newSalt()
            settingsRepository.setPin(PinHasher.hash(pin, salt), salt)
            lockManager.markUnlocked()
        }
    }

    fun turnOff() {
        viewModelScope.launch { settingsRepository.clearLock() }
    }

    fun setBiometric(v: Boolean) {
        viewModelScope.launch { settingsRepository.setBiometric(v) }
    }

    fun setHideAmounts(v: Boolean) {
        viewModelScope.launch { settingsRepository.setHideAmounts(v) }
    }
}

private enum class PinStep { NONE, ENTER, CONFIRM }

@Composable
fun AppLockSettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AppLockSettingsViewModel = hiltViewModel(),
) {
    val s by viewModel.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var step by remember { mutableStateOf(PinStep.NONE) }
    var firstPin by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }
    var confirmOff by remember { mutableStateOf(false) }
    val mismatch = stringResource(R.string.lock_pin_mismatch)

    if (step != PinStep.NONE) {
        Scaffold(topBar = { KirayaTopBar(stringResource(R.string.lock_set_pin), onBack = { step = PinStep.NONE }) }) { padding ->
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                PinPad(
                    title = stringResource(if (step == PinStep.ENTER) R.string.lock_new_pin else R.string.lock_confirm_pin),
                    error = pinError,
                    clearKey = step,
                    onComplete = { pin ->
                        if (step == PinStep.ENTER) {
                            firstPin = pin
                            pinError = null
                            step = PinStep.CONFIRM
                        } else if (pin == firstPin) {
                            viewModel.savePin(pin)
                            firstPin = ""
                            pinError = null
                            step = PinStep.NONE
                        } else {
                            pinError = mismatch
                            step = PinStep.ENTER
                        }
                    },
                )
            }
        }
        return
    }

    Scaffold(
        modifier = modifier,
        topBar = { KirayaTopBar(title = stringResource(R.string.lock_title), onBack = onBack) },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.lock_pin_title), style = MaterialTheme.typography.titleMedium)
                    Text(
                        stringResource(if (s.lockEnabled) R.string.lock_is_on else R.string.lock_is_off),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (s.lockEnabled) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { step = PinStep.ENTER }) { Text(stringResource(R.string.lock_change_pin)) }
                            TextButton(onClick = { confirmOff = true }) {
                                Text(stringResource(R.string.lock_turn_off), color = MaterialTheme.colorScheme.error)
                            }
                        }
                        if (Biometrics.isAvailable(context)) {
                            SwitchRow(
                                title = stringResource(R.string.lock_biometric),
                                subtitle = stringResource(R.string.lock_biometric_hint),
                                checked = s.biometricEnabled,
                                onChange = viewModel::setBiometric,
                            )
                        }
                    } else {
                        Button(onClick = { step = PinStep.ENTER }) { Text(stringResource(R.string.lock_set_pin)) }
                    }
                }
            }
            OutlinedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    SwitchRow(
                        title = stringResource(R.string.lock_hide_amounts),
                        subtitle = stringResource(R.string.lock_hide_amounts_hint),
                        checked = s.hideAmounts,
                        onChange = viewModel::setHideAmounts,
                    )
                }
            }
        }
    }

    if (confirmOff) {
        ConfirmDialog(
            title = stringResource(R.string.lock_turn_off),
            message = stringResource(R.string.lock_turn_off_message),
            confirmLabel = stringResource(R.string.lock_turn_off),
            onConfirm = viewModel::turnOff,
            onDismiss = { confirmOff = false },
        )
    }
}

@Composable
fun SwitchRow(title: String, subtitle: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
