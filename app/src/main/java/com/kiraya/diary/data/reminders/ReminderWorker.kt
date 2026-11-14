package com.kiraya.diary.data.reminders

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kiraya.diary.R
import com.kiraya.diary.core.date.DateUtils
import com.kiraya.diary.core.money.Money
import com.kiraya.diary.data.settings.SettingsRepository
import com.kiraya.diary.domain.repository.TenantRepository
import com.kiraya.diary.domain.usecase.DueTenantsUseCase
import com.kiraya.diary.domain.usecase.GenerateRentEntriesUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/** Runs every morning: rent due/overdue notification and lease-ending alert. */
@HiltWorker
class ReminderWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val settingsRepository: SettingsRepository,
    private val generateRentEntries: GenerateRentEntriesUseCase,
    private val dueTenants: DueTenantsUseCase,
    private val tenantRepository: TenantRepository,
    private val notifier: ReminderNotifier,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val settings = settingsRepository.current()
        if (!settings.remindersEnabled || !notifier.canNotify()) return Result.success()
        return runCatching {
            // Make sure this month's rent exists before checking dues.
            generateRentEntries()
            val now = DateUtils.now()

            val due = dueTenants.get(settings.reminderDaysBefore, now)
            if (due.isNotEmpty()) {
                val total = due.sumOf { it.amount }
                val overdue = due.count { it.overdue }
                notifier.showRentDue(
                    title = context.resources.getQuantityString(R.plurals.notif_rent_title, due.size, due.size),
                    text = if (overdue > 0) {
                        context.getString(R.string.notif_rent_text_overdue, Money.formatWithSymbol(total, settings.currencySymbol), overdue)
                    } else {
                        context.getString(R.string.notif_rent_text, Money.formatWithSymbol(total, settings.currencySymbol))
                    },
                    lines = due.map { "${it.tenant.tenant.name}: ${Money.formatWithSymbol(it.amount, settings.currencySymbol)}" },
                )
            }

            val ending = tenantRepository.getLeasesEndingBefore(now + settings.leaseAlertDays * DateUtils.DAY_MILLIS)
            if (ending.isNotEmpty()) {
                notifier.showLeaseEnding(
                    title = context.resources.getQuantityString(R.plurals.notif_lease_title, ending.size, ending.size),
                    text = context.getString(R.string.notif_lease_text),
                    lines = ending.map { a ->
                        val days = DateUtils.daysBetween(now, a.lease.endDate ?: now)
                        if (days < 0) {
                            context.getString(R.string.notif_lease_line_ended, a.tenantName)
                        } else {
                            context.getString(R.string.notif_lease_line, a.tenantName, DateUtils.formatDate(a.lease.endDate ?: now))
                        }
                    },
                )
            }
        }.fold(onSuccess = { Result.success() }, onFailure = { Result.retry() })
    }

    companion object {
        const val NAME = "daily_reminders"
    }
}
