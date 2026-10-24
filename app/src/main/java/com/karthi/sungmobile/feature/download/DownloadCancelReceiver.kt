package com.karthi.sungmobile.feature.download

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Notification actions stop the transfer even when the Library screen is gone. */
class DownloadCancelReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getIntExtra("requestId", -1)
        if (id >= 0) DownloadRequests.cancel(context, id)
    }
}
