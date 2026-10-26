package com.karthi.sungmobile.feature.download

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PersistableBundle
import androidx.core.content.ContextCompat
import com.karthi.sungmobile.core.model.CatalogItem
import com.karthi.sungmobile.core.model.CatalogKind
import com.karthi.sungmobile.core.model.library.libraryKeyOrNull
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.Job

internal data class DownloadRequest(val id: Int, val track: CatalogItem) {
    fun extras() = PersistableBundle().apply {
        putString("trackId", track.id); putString("title", track.title)
        putString("artist", track.subtitle); putString("url", track.url)
        putString("artwork", track.artworkUrl)
    }

    companion object {
        fun from(id: Int, extras: PersistableBundle): DownloadRequest? {
            val trackId = extras.getString("trackId") ?: return null
            val title = extras.getString("title") ?: return null
            val url = extras.getString("url") ?: return null
            return DownloadRequest(id, CatalogItem(trackId, CatalogKind.SONG, title,
                extras.getString("artist").orEmpty(), url, artworkUrl = extras.getString("artwork")))
        }
    }
}

/** Ephemeral running-request guard. Completed files are found by scanning MediaStore. */
internal object DownloadRequests {
    private val nextId = AtomicInteger(600_000_000 + (System.currentTimeMillis() % 100_000_000).toInt())
    private val tracks = mutableMapOf<String, Int>()
    private val pending = mutableMapOf<Int, DownloadRequest>()
    private val cancelled = mutableSetOf<Int>()
    private val active = mutableMapOf<Int, Pair<AudioTransfer, Job>>()

    @Synchronized fun begin(context: Context, track: CatalogItem): String {
        if (Build.VERSION.SDK_INT < 29) return "Downloads require Android 10 or newer."
        if (track.libraryKeyOrNull() == null) return "Only online songs can be downloaded."
        if (track.id in tracks) return "This song is already downloading."
        val id = nextId.incrementAndGet()
        val request = DownloadRequest(id, track)
        tracks[track.id] = id
        pending[id] = request
        return try {
            DownloadNotifications(context).showQueued(request)
            if (Build.VERSION.SDK_INT >= 34) {
                val scheduler = context.getSystemService(JobScheduler::class.java)
                val job = JobInfo.Builder(id, ComponentName(context, AudioDownloadJob::class.java))
                    .setUserInitiated(true).setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                    .setExtras(request.extras()).build()
                check(scheduler.schedule(job) == JobScheduler.RESULT_SUCCESS)
            } else {
                ContextCompat.startForegroundService(context, Intent(context, AudioDownloadService::class.java)
                    .setAction(AudioDownloadService.START).putExtra("requestId", id)
                    .putExtra("trackId", track.id).putExtra("title", track.title)
                    .putExtra("artist", track.subtitle).putExtra("url", track.url)
                    .putExtra("artwork", track.artworkUrl))
            }
            "Download started. Watch its notification for progress."
        } catch (_: Exception) {
            tracks.remove(track.id)
            pending.remove(id)
            DownloadNotifications(context).clear(id)
            "Could not start the download. Try again while SungMobile is open."
        }
    }

    @Synchronized fun running(request: DownloadRequest, transfer: AudioTransfer, job: Job) {
        pending.remove(request.id)
        active[request.id] = transfer to job
        tracks[request.track.id] = request.id
    }

    @Synchronized fun accept(id: Int): Boolean = !cancelled.remove(id)

    @Synchronized fun finish(request: DownloadRequest) {
        active.remove(request.id)
        pending.remove(request.id)
        cancelled.remove(request.id)
        if (tracks[request.track.id] == request.id) tracks.remove(request.track.id)
    }

    @Synchronized fun cancel(context: Context, id: Int) {
        cancelled += id
        val waiting = pending.remove(id)
        if (waiting != null) {
            if (tracks[waiting.track.id] == id) tracks.remove(waiting.track.id)
            DownloadNotifications(context).finished(waiting, false, "Download cancelled")
        }
        stop(id)
        if (Build.VERSION.SDK_INT >= 34) context.getSystemService(JobScheduler::class.java).cancel(id)
    }

    @Synchronized fun stop(id: Int) {
        active[id]?.let { (transfer, job) -> transfer.cancel(); job.cancel() }
    }
}
