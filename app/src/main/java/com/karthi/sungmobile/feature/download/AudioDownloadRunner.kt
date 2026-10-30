package com.karthi.sungmobile.feature.download

import android.content.Context
import com.karthi.sungmobile.SungApplication
import kotlinx.coroutines.CancellationException

/** Shared transfer outcome handling for the service and user-initiated job. */
internal suspend fun runAudioDownload(
    context: Context,
    request: DownloadRequest,
    transfer: AudioTransfer,
    notifications: DownloadNotifications,
) {
    try {
        transfer.run(request.track) { bytes, total -> notifications.showProgress(request, bytes, total) }
        (context.applicationContext as SungApplication).container.localAudio.invalidate()
        notifications.finished(request, true, "Saved to Music/SungMobile")
    } catch (_: CancellationException) {
        notifications.finished(request, false, "Download cancelled")
    } catch (error: Exception) {
        if (!transfer.isCancelled) logDownloadFailure(error)
        notifications.finished(request, false, if (transfer.isCancelled) "Download cancelled"
            else downloadFailureMessage(error))
    }
}
