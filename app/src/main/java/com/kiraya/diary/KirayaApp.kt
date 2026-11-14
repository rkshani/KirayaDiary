package com.kiraya.diary

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.kiraya.diary.core.coroutines.ApplicationScope
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

    @Inject @ApplicationScope
    lateinit var appScope: CoroutineScope

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        workScheduler.scheduleAll()
        // Catch up right away so the new month's rent shows without waiting for the worker.
        appScope.launch { runCatching { generateRentEntries() } }
    }
}
