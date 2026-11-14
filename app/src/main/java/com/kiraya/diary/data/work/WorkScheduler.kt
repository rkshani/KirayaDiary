package com.kiraya.diary.data.work

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.kiraya.diary.data.reminders.ReminderWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val workManager get() = WorkManager.getInstance(context)

    /** Schedules the daily background jobs. Safe to call on every app start. */
    fun scheduleAll() {
        workManager.enqueueUniquePeriodicWork(
            RentWorker.NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<RentWorker>(1, TimeUnit.DAYS).build(),
        )
        // Reminder notifications are hidden for now. Turn back on in a later update.
        // workManager.enqueueUniquePeriodicWork(
        //     ReminderWorker.NAME,
        //     ExistingPeriodicWorkPolicy.KEEP,
        //     PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS)
        //         .setInitialDelay(millisUntil(REMINDER_HOUR), TimeUnit.MILLISECONDS)
        //         .build(),
        // )
        workManager.cancelUniqueWork(ReminderWorker.NAME)
    }

    /** Runs rent generation now (for example after adding a tenant). */
    fun generateRentNow() {
        workManager.enqueueUniqueWork(
            RentWorker.NAME + "_now",
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<RentWorker>().build(),
        )
    }

    /** Sends the reminder notifications now (used by the "test" button). */
    fun remindNow() {
        workManager.enqueueUniqueWork(
            ReminderWorker.NAME + "_now",
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<ReminderWorker>().build(),
        )
    }

    private fun millisUntil(hour: Int): Long {
        val now = Calendar.getInstance()
        val next = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (!after(now)) add(Calendar.DAY_OF_MONTH, 1)
        }
        return next.timeInMillis - now.timeInMillis
    }

    companion object {
        /** Reminders go out around 9 AM. */
        const val REMINDER_HOUR = 9
    }
}
