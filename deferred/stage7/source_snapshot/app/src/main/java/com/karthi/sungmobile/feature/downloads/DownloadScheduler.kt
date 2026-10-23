package com.karthi.sungmobile.feature.downloads

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit

object DownloadScheduler {
    fun enqueue(context: Context, videoId: String) {
        val request = OneTimeWorkRequestBuilder<AudioDownloadWorker>()
            .setInputData(workDataOf(AudioDownloadWorker.VIDEO_ID to videoId))
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(androidx.work.BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .addTag("audio-download")
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork("audio-$videoId", ExistingWorkPolicy.KEEP, request)
    }

    fun cancel(context: Context, videoId: String) {
        WorkManager.getInstance(context).cancelUniqueWork("audio-$videoId")
    }
}
