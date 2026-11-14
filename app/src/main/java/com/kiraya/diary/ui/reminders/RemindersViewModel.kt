package com.kiraya.diary.ui.reminders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kiraya.diary.core.date.DateUtils
import com.kiraya.diary.data.reminders.ReminderMessageBuilder
import com.kiraya.diary.data.settings.AppSettings
import com.kiraya.diary.data.settings.SettingsRepository
import com.kiraya.diary.data.work.WorkScheduler
import com.kiraya.diary.domain.model.LeaseAlert
import com.kiraya.diary.domain.repository.TenantRepository
import com.kiraya.diary.domain.usecase.DueTenant
import com.kiraya.diary.domain.usecase.DueTenantsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RemindersState(
    val loaded: Boolean = false,
    val settings: AppSettings = AppSettings(),
    val due: List<DueTenant> = emptyList(),
    val leases: List<LeaseAlert> = emptyList(),
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class RemindersViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val messageBuilder: ReminderMessageBuilder,
    private val workScheduler: WorkScheduler,
    dueTenants: DueTenantsUseCase,
    tenantRepository: TenantRepository,
) : ViewModel() {
    val state: StateFlow<RemindersState> = settingsRepository.settings.flatMapLatest { s ->
        combine(
            dueTenants.observe(s.reminderDaysBefore),
            tenantRepository.observeLeasesEndingBefore(DateUtils.now() + s.leaseAlertDays * DateUtils.DAY_MILLIS),
        ) { due, leases -> RemindersState(true, s, due, leases) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RemindersState())

    fun setEnabled(v: Boolean) = update { it.copy(remindersEnabled = v) }
    fun setDaysBefore(v: Int) = update { it.copy(reminderDaysBefore = v) }
    fun setLeaseAlertDays(v: Int) = update { it.copy(leaseAlertDays = v) }
    fun setMessageLanguage(v: String) = update { it.copy(messageLanguage = v) }

    fun testNow() = workScheduler.remindNow()

    fun message(due: DueTenant, language: String = state.value.settings.messageLanguage): String {
        val s = state.value.settings
        return messageBuilder.build(
            language = language,
            tenantName = due.tenant.tenant.name,
            amount = due.amount,
            months = due.months,
            currencySymbol = s.currencySymbol,
            landlordName = s.landlordName,
            unitName = due.tenant.unitName,
        )
    }

    private fun update(change: (AppSettings) -> AppSettings) {
        viewModelScope.launch {
            val n = change(settingsRepository.current())
            settingsRepository.setReminders(n.remindersEnabled, n.reminderDaysBefore, n.leaseAlertDays, n.messageLanguage)
        }
    }
}
