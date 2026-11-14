package com.kiraya.diary.ui.backup

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.kiraya.diary.R
import com.kiraya.diary.core.date.DateUtils
import com.kiraya.diary.data.backup.BackupManager
import com.kiraya.diary.ui.components.ConfirmDialog
import com.kiraya.diary.ui.components.KirayaTopBar
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class BackupState(
    val busy: Boolean = false,
    val message: Int? = null,
    /** A backup picked for restore, waiting for the user to confirm. */
    val pendingRestore: Uri? = null,
    val pendingInfo: BackupManager.BackupInfo? = null,
)

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val backupManager: BackupManager,
) : ViewModel() {
    private val _state = MutableStateFlow(BackupState())
    val state: StateFlow<BackupState> = _state.asStateFlow()

    fun backup(uri: Uri) = run {
        backupManager.backup(uri)
        _state.update { it.copy(message = R.string.backup_done) }
    }

    fun pickRestore(uri: Uri) = run {
        val info = backupManager.inspect(uri)
        _state.update { it.copy(pendingRestore = uri, pendingInfo = info) }
    }

    fun confirmRestore() {
        val uri = _state.value.pendingRestore ?: return
        _state.update { it.copy(pendingRestore = null) }
        run {
            backupManager.restore(uri)
            backupManager.restartApp()
        }
    }

    fun cancelRestore() = _state.update { it.copy(pendingRestore = null, pendingInfo = null) }

    private fun run(block: suspend () -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(busy = true, message = null) }
            val ok = runCatching { block() }
            _state.update {
                it.copy(
                    busy = false,
                    message = if (ok.isFailure) {
                        if (ok.exceptionOrNull() is BackupManager.InvalidBackupException) R.string.backup_invalid else R.string.backup_failed
                    } else {
                        it.message
                    },
                )
            }
        }
    }
}

@Composable
fun BackupScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BackupViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val create = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null) viewModel.backup(uri)
    }
    val open = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.pickRestore(uri)
    }

    Scaffold(
        modifier = modifier,
        topBar = { KirayaTopBar(title = stringResource(R.string.backup_title), onBack = onBack) },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(stringResource(R.string.backup_info), style = MaterialTheme.typography.bodyMedium)
            Button(
                onClick = {
                    val stamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
                    create.launch("kiraya_backup_$stamp.zip")
                },
                enabled = !state.busy,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.backup_create)) }
            OutlinedButton(
                onClick = { open.launch(arrayOf("application/zip", "application/octet-stream", "*/*")) },
                enabled = !state.busy,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.backup_restore)) }
            if (state.busy) Text(stringResource(R.string.backup_working))
            state.message?.let { Text(stringResource(it), style = MaterialTheme.typography.titleSmall) }
        }
    }

    if (state.pendingRestore != null) {
        val info = state.pendingInfo
        ConfirmDialog(
            title = stringResource(R.string.backup_restore),
            message = stringResource(
                R.string.backup_restore_message,
                info?.let { DateUtils.formatDate(it.createdAt) }.orEmpty(),
            ),
            confirmLabel = stringResource(R.string.backup_restore),
            onConfirm = viewModel::confirmRestore,
            onDismiss = viewModel::cancelRestore,
        )
    }
}
