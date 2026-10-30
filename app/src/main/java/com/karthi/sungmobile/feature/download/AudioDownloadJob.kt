package com.karthi.sungmobile.feature.download

import android.app.job.JobParameters
import android.app.job.JobService
import android.os.Build
import androidx.annotation.RequiresApi
import com.karthi.sungmobile.SungApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/** Android 14+: system-owned user-initiated transfer continues after the UI closes. */
@RequiresApi(34)
class AudioDownloadJob : JobService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val stopped = mutableSetOf<Int>()
    private val jobs = mutableMapOf<Int, Job>()

    override fun onStartJob(params: JobParameters): Boolean {
        val request = DownloadRequest.from(params.jobId, params.extras) ?: return false
        val notifier = DownloadNotifications(this)
        if (!DownloadRequests.accept(request.id)) {
            notifier.finished(request, false, "Download cancelled")
            DownloadRequests.finish(request)
            return false
        }
        setNotification(params, request.id, notifier.active(request), JOB_END_NOTIFICATION_POLICY_DETACH)
        val catalog = (application as SungApplication).container.catalog
        val transfer = AudioTransfer(this, catalog)
        val job = scope.launch(start = CoroutineStart.LAZY) {
            try {
                runAudioDownload(this@AudioDownloadJob, request, transfer, notifier)
            } finally {
                DownloadRequests.finish(request)
                jobs.remove(request.id)
                if (stopped.remove(request.id).not()) jobFinished(params, false)
            }
        }
        jobs[request.id] = job
        DownloadRequests.running(request, transfer, job)
        job.start()
        return true
    }

    override fun onStopJob(params: JobParameters): Boolean {
        stopped += params.jobId
        DownloadRequests.stop(params.jobId)
        return false // No automatic retry or hidden inventory; the user can request it again.
    }

    override fun onDestroy() { jobs.values.forEach(Job::cancel); scope.cancel(); super.onDestroy() }
}
