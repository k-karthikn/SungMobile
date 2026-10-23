package com.karthi.sungmobile.updates

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.karthi.sungmobile.MainActivity
import com.karthi.sungmobile.R

internal object UpdateNotifications {
    fun cancel(context: Context) { context.getSystemService(NotificationManager::class.java).cancel(9403) }
    fun ready(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return false
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel("app_updates", "App updates", NotificationManager.IMPORTANCE_DEFAULT))
        val open = PendingIntent.getActivity(context, 9403, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        manager.notify(9403, NotificationCompat.Builder(context, "app_updates")
            .setSmallIcon(R.drawable.ic_notification).setContentTitle("Sung update ready")
            .setContentText("Open Sung to review and install the update.").setContentIntent(open).setAutoCancel(true).setOnlyAlertOnce(true).build())
        return true
    }
}
