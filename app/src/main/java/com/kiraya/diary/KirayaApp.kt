package com.kiraya.diary

import android.app.Application
import android.content.Context
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.kiraya.diary.core.coroutines.ApplicationScope
import com.kiraya.diary.core.locale.LocaleHelper
import com.kiraya.diary.data.reminders.ReminderNotifier
import com.kiraya.diary.data.work.WorkScheduler
import com.kiraya.diary.domain.usecase.GenerateRentEntriesUseCase
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class KirayaApp : Application(), Configuration.Provider {
    @Inject lateinit var workerFactory: HiltWorkerFactory

    @Inject lateinit var workScheduler: WorkScheduler

    @Inject lateinit var generateRentEntries: GenerateRentEntriesUseCase

    @Inject lateinit var reminderNotifier: ReminderNotifier

    @Inject @ApplicationScope
    lateinit var appScope: CoroutineScope

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun attachBaseContext(base: Context) {
        // Notifications and receipts made in the background use the app language too.
        super.attachBaseContext(LocaleHelper.wrap(base))
    }

    override fun onCreate() {
        super.onCreate()
        reminderNotifier.createChannels()
        workScheduler.scheduleAll()
        // Catch up right away so the new month's rent shows without waiting for the worker.
        appScope.launch { runCatching { generateRentEntries() } }
    }
}
