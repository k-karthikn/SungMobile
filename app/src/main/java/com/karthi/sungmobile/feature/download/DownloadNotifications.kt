package com.karthi.sungmobile.feature.download

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.karthi.sungmobile.R

private const val CHANNEL = "sungmobile_audio_downloads"

internal class DownloadNotifications(private val context: Context) {
    private val manager = context.getSystemService(NotificationManager::class.java).also { notifications ->
        notifications.createNotificationChannel(
            NotificationChannel(CHANNEL, "Music downloads", NotificationManager.IMPORTANCE_LOW))
    }

    fun active(request: DownloadRequest, bytes: Long = 0, total: Long? = null): Notification {
        val percent = total?.takeIf { it > 0 }?.let { (bytes * 100 / it).toInt().coerceIn(0, 100) }
        val cancel = PendingIntent.getBroadcast(context, request.id,
            Intent(context, DownloadCancelReceiver::class.java).putExtra("requestId", request.id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_download)
            .setContentTitle(request.track.title)
            .setContentText(percent?.let { "Downloading $it%" } ?: if (bytes > 0) "Downloading • size unknown"
                else "Preparing audio…")
            .setProgress(100, percent ?: 0, percent == null)
            .setOngoing(true).setOnlyAlertOnce(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Cancel", cancel)
            .build()
    }

    fun showQueued(request: DownloadRequest) { manager.notify(request.id, active(request)) }
    fun showProgress(request: DownloadRequest, bytes: Long, total: Long?) {
        manager.notify(request.id, active(request, bytes, total))
    }
    fun finished(request: DownloadRequest, success: Boolean, message: String) {
        manager.notify(request.id, NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_download).setContentTitle(request.track.title)
            .setContentText(message).setAutoCancel(true).setOnlyAlertOnce(true)
            .setOngoing(false).build())
    }
    fun clear(id: Int) { manager.cancel(id) }
}
