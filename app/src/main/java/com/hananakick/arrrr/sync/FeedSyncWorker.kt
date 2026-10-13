package com.hananakick.arrrr.sync

import android.content.Context
import androidx.work.*
import com.hananakick.arrrr.ReaderApplication
import com.hananakick.arrrr.data.ReaderSettings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

class FeedSyncWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        val app = applicationContext as ReaderApplication
        val settings=app.settings.flow.first()
        if (!settings.background) return Result.success()
        return try {
            val failures=app.repository.refresh(settings)
            if (failures>0 && runAttemptCount<3) Result.retry() else Result.success()
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { if (runAttemptCount<3) Result.retry() else Result.failure() }
    }
}

object SyncScheduler {
    const val WORK_NAME = "feed-poll"
    fun schedule(context: Context, settings: ReaderSettings) {
        val manager=WorkManager.getInstance(context)
        if (!settings.background) { manager.cancelUniqueWork(WORK_NAME); return }
        val request=PeriodicWorkRequestBuilder<FeedSyncWorker>(settings.pollMinutes.toLong(), TimeUnit.MINUTES)
            .setInitialDelay(settings.pollMinutes.toLong(), TimeUnit.MINUTES)
            .setConstraints(Constraints.Builder()
                .setRequiredNetworkType(if (settings.wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        manager.enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
    }
}
