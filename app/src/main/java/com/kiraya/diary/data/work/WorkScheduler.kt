package com.kiraya.diary.data.work

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
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
    }

    /** Runs rent generation now (for example after adding a tenant). */
    fun generateRentNow() {
        workManager.enqueueUniqueWork(
            RentWorker.NAME + "_now",
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<RentWorker>().build(),
        )
    }
}
