package com.kiraya.diary.ui.receipts

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.kiraya.diary.R
import com.kiraya.diary.data.receipt.ReceiptData
import com.kiraya.diary.data.receipt.ReceiptGenerator
import com.kiraya.diary.data.settings.SettingsRepository
import com.kiraya.diary.domain.repository.RentRepository
import com.kiraya.diary.domain.repository.TenantRepository
import com.kiraya.diary.domain.usecase.ObserveTenantLedgerUseCase
import com.kiraya.diary.ui.components.KirayaTopBar
import com.kiraya.diary.ui.navigation.Routes
import com.kiraya.diary.ui.util.Share
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

data class ReceiptState(
    val file: File? = null,
    val preview: Bitmap? = null,
    val phone: String = "",
    val failed: Boolean = false,
)

@HiltViewModel
class ReceiptViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val rentRepository: RentRepository,
    private val tenantRepository: TenantRepository,
    private val observeLedger: ObserveTenantLedgerUseCase,
    private val generator: ReceiptGenerator,
    private val settings: SettingsRepository,
) : ViewModel() {
    private val paymentId: Long = checkNotNull(savedStateHandle[Routes.ARG_PAYMENT_ID])
    private val _state = MutableStateFlow(ReceiptState())
    val state: StateFlow<ReceiptState> = _state.asStateFlow()

    /** Called with the screen's context so the receipt uses the app language. */
    fun build(localized: android.content.Context) {
        if (_state.value.file != null) return
        viewModelScope.launch {
            runCatching {
                val payment = checkNotNull(rentRepository.getPayment(paymentId))
                val tenant = tenantRepository.observeTenant(payment.tenantId).filterNotNull().first()
                val ledger = observeLedger(payment.tenantId).first()
                val s = settings.current()
                val file = generator.create(
                    ReceiptData(payment, tenant, ledger.balance, s.landlordName, s.currencySymbol),
                    localized,
                )
                _state.value = ReceiptState(file, renderPreview(file), tenant.tenant.phone)
            }.onFailure { _state.value = ReceiptState(failed = true) }
        }
    }

    private suspend fun renderPreview(file: File): Bitmap? = withContext(Dispatchers.IO) {
        runCatching {
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { fd ->
                PdfRenderer(fd).use { renderer ->
                    renderer.openPage(0).use { page ->
                        val bmp = Bitmap.createBitmap(page.width * 2, page.height * 2, Bitmap.Config.ARGB_8888)
                        bmp.eraseColor(Color.WHITE)
                        page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        bmp
                    }
                }
            }
        }.getOrNull()
    }
}

@Composable
fun ReceiptScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ReceiptViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.build(context) }
    val chooser = stringResource(R.string.receipt_share_title)

    Scaffold(
        modifier = modifier,
        topBar = { KirayaTopBar(title = stringResource(R.string.receipt_title), onBack = onBack) },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val preview = state.preview
            when {
                state.failed -> Text(stringResource(R.string.receipt_failed))
                preview == null -> CircularProgressIndicator(Modifier.padding(32.dp))
                else -> ElevatedCard(Modifier.fillMaxWidth()) {
                    Image(
                        preview.asImageBitmap(),
                        contentDescription = stringResource(R.string.receipt_title),
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(preview.width.toFloat() / preview.height),
                    )
                }
            }
            val file = state.file
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = {
                        file?.let { Share.shareFileToWhatsApp(context, it, "application/pdf", state.phone, chooser) }
                    },
                    enabled = file != null,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(painterResource(R.drawable.ic_message), contentDescription = null)
                    Text(stringResource(R.string.receipt_whatsapp), Modifier.padding(start = 8.dp))
                }
                OutlinedButton(
                    onClick = { file?.let { Share.shareFile(context, it, "application/pdf", chooser) } },
                    enabled = file != null,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(painterResource(R.drawable.ic_share), contentDescription = null)
                    Text(stringResource(R.string.action_share), Modifier.padding(start = 8.dp))
                }
            }
        }
    }
}
