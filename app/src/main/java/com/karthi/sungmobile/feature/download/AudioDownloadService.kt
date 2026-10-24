package com.karthi.sungmobile.feature.download

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import com.karthi.sungmobile.SungApplication
import com.karthi.sungmobile.core.model.CatalogItem
import com.karthi.sungmobile.core.model.CatalogKind
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/** Android 10–13 fallback: user-started dataSync foreground service. */
class AudioDownloadService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val jobs = mutableMapOf<Int, Pair<DownloadRequest, Job>>()
    private var foregroundId: Int? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (Build.VERSION.SDK_INT < 29 || Build.VERSION.SDK_INT > 33 || intent?.action != START) {
            stopSelfResult(startId)
            return START_NOT_STICKY
        }
        val id = intent.getIntExtra("requestId", -1)
        val trackId = intent.getStringExtra("trackId")
        val url = intent.getStringExtra("url")
        if (id < 0 || trackId == null || url == null) {
            stopSelfResult(startId)
            return START_NOT_STICKY
        }
        if (id in jobs) return START_NOT_STICKY
        val request = DownloadRequest(id, CatalogItem(trackId, CatalogKind.SONG,
            intent.getStringExtra("title").orEmpty(), intent.getStringExtra("artist").orEmpty(), url))
        val notifier = DownloadNotifications(this)
        if (!DownloadRequests.accept(id)) {
            notifier.finished(request, false, "Download cancelled")
            DownloadRequests.finish(request)
            stopSelfResult(startId)
            return START_NOT_STICKY
        }
        if (foregroundId == null) {
            startForeground(id, notifier.active(request), ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
            foregroundId = id
        } else notifier.showQueued(request)
        val catalog = (application as SungApplication).container.catalog
        val transfer = AudioTransfer(this, catalog)
        val job = scope.launch(start = CoroutineStart.LAZY) {
            try {
                transfer.run(request.track) { bytes, total -> notifier.showProgress(request, bytes, total) }
                notifier.finished(request, true, "Saved to Music/SungMobile")
            } catch (_: CancellationException) {
                notifier.finished(request, false, "Download cancelled")
            } catch (_: Exception) {
                notifier.finished(request, false, if (transfer.isCancelled) "Download cancelled"
                    else "Download failed. Tap Download to try again.")
            } finally {
                DownloadRequests.finish(request)
                jobs.remove(id)
                if (foregroundId == id) {
                    val next = jobs.values.firstOrNull()?.first
                    if (next != null) {
                        startForeground(next.id, notifier.active(next), ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
                        foregroundId = next.id
                    } else {
                        stopForeground(STOP_FOREGROUND_DETACH)
                        foregroundId = null
                    }
                }
                if (jobs.isEmpty()) stopSelf()
            }
        }
        jobs[id] = request to job
        DownloadRequests.running(request, transfer, job)
        job.start()
        return START_NOT_STICKY
    }

    override fun onDestroy() { jobs.values.forEach { it.second.cancel() }; scope.cancel(); super.onDestroy() }

    companion object { const val START = "com.karthi.sungmobile.download.START" }
}
