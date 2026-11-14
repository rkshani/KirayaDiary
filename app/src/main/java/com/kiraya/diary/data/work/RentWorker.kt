package com.kiraya.diary.data.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kiraya.diary.domain.usecase.GenerateRentEntriesUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/** Runs once a day and adds the new month's rent entries when a month starts. */
@HiltWorker
class RentWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val generateRentEntries: GenerateRentEntriesUseCase,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = runCatching { generateRentEntries() }
        .fold(onSuccess = { Result.success() }, onFailure = { Result.retry() })

    companion object {
        const val NAME = "monthly_rent"
    }
}
