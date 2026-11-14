package com.kiraya.diary.ui.reports

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kiraya.diary.data.export.ReportExporter
import com.kiraya.diary.data.settings.SettingsRepository
import com.kiraya.diary.domain.usecase.ObserveReportUseCase
import com.kiraya.diary.domain.usecase.Period
import com.kiraya.diary.domain.usecase.Report
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

enum class ExportType { CSV, PDF }

@HiltViewModel
class ReportsViewModel @Inject constructor(
    observeReport: ObserveReportUseCase,
    private val exporter: ReportExporter,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    private val period = MutableStateFlow(Period.currentMonth())

    val report: StateFlow<Report?> = observeReport(period)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val periodState: StateFlow<Period> = period.asStateFlow()

    private val _exporting = MutableStateFlow(false)
    val exporting: StateFlow<Boolean> = _exporting.asStateFlow()

    fun previous() = period.update { it.previous() }
    fun next() = period.update { p -> p.next().takeUnless { it.isAfterNow() } ?: p }
    fun showYear(year: Boolean) = period.update { if (year) it.asYear() else it.asMonth() }

    /** Writes the file and hands it to [onReady] for sharing. [ctx] carries the app language. */
    fun export(type: ExportType, ctx: Context, onReady: (File, String) -> Unit) {
        val r = report.value ?: return
        viewModelScope.launch {
            _exporting.value = true
            runCatching {
                val symbol = settingsRepository.current().currencySymbol
                when (type) {
                    ExportType.CSV -> exporter.csv(r, ctx, symbol) to "text/csv"
                    ExportType.PDF -> exporter.pdf(r, ctx, symbol) to "application/pdf"
                }
            }.onSuccess { (file, mime) -> onReady(file, mime) }
            _exporting.value = false
        }
    }
}
