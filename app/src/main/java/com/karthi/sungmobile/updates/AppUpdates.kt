package com.karthi.sungmobile.updates

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import org.json.JSONObject
import java.io.File

/** One updater per process. Files and preferences are private; installation is always user initiated. */
class AppUpdates internal constructor(private val context: Context) {
    private val prefs = context.getSharedPreferences("app_updates", Context.MODE_PRIVATE)
    private val directory = File(context.filesDir, "updates").apply { mkdirs() }
    private val apk = File(directory, "update.apk")
    private val manifestFile = File(directory, "update.json")
    private val lock = Mutex()
    private val mutable = MutableStateFlow(UpdateState(enabled = enabled(),
        ready = apk.exists() && manifestFile.exists(), message = prefs.getString("message", "Updates have not been checked yet.").orEmpty()))
    val state = mutable.asStateFlow()
    fun enabled() = prefs.getBoolean("enabled", true)
    fun setEnabled(value: Boolean) {
        prefs.edit().putBoolean("enabled", value).apply()
        mutable.value = mutable.value.copy(enabled = value)
        UpdateSchedule.configure(context, value)
        if (value) UpdateSchedule.checkSoon(context)
    }
    fun start() {
        // After an APK update, discard the already-installed download even while auto checks are off.
        if (manifestFile.exists()) runCatching {
            val manifest = UpdateManifest.parse(manifestFile.readText())
            if (!manifest.newerThan(UpdateApk.code(UpdateApk.installed(context)), context.packageName)) {
                apk.delete(); manifestFile.delete(); UpdateNotifications.cancel(context)
                message("The downloaded update is already installed.", false)
            }
        }
        UpdateSchedule.configure(context, enabled())
        if (enabled() && System.currentTimeMillis() - prefs.getLong("checked", 0) > 12 * 60 * 60 * 1000L)
            UpdateSchedule.checkSoon(context)
    }
    private fun message(text: String, ready: Boolean = mutable.value.ready) {
        prefs.edit().putString("message", text).apply()
        mutable.value = mutable.value.copy(message = text, ready = ready)
    }
    suspend fun check(automatic: Boolean): Boolean = withContext(Dispatchers.IO) {
        if (!lock.tryLock()) return@withContext true
        mutable.value = mutable.value.copy(busy = true)
        val partial = File(directory, "update.part")
        try {
            if (automatic && !enabled()) return@withContext true
            message("Checking GitHub for updates…")
            // latest intentionally excludes prereleases. Ordering is decided by the manifest's Android code.
            val (status, body) = UpdateHttp.json("https://api.github.com/repos/k-karthikn/SungMobile/releases/latest")
            if (status == 404) { message("No stable release is published yet."); return@withContext true }
            kotlin.check(status == 200) { "GitHub update check failed (HTTP $status). Try again later." }
            val release = JSONObject(body)
            kotlin.check(!release.optBoolean("draft") && !release.optBoolean("prerelease"))
            val assets = release.getJSONArray("assets")
            val metadata = (0 until assets.length()).map { assets.getJSONObject(it) }.singleOrNull { it.optString("name") == "update.json" }
            if (metadata == null) { message("This release does not support automatic updates yet."); return@withContext true }
            val manifestUrl = metadata.getString("browser_download_url")
            require(isReleaseAsset(manifestUrl))
            val (manifestStatus, text) = UpdateHttp.json(manifestUrl)
            kotlin.check(manifestStatus == 200) { "Could not read the update details." }
            val manifest = UpdateManifest.parse(text)
            require((0 until assets.length()).any { assets.getJSONObject(it).optString("browser_download_url") == manifest.apkUrl })
            if (!manifest.newerThan(UpdateApk.code(UpdateApk.installed(context)), context.packageName)) {
                apk.delete(); manifestFile.delete(); UpdateNotifications.cancel(context)
                message("No newer compatible update is available.", false)
                return@withContext true
            }
            if (apk.exists() && manifestFile.exists() && manifestFile.readText() == text) {
                UpdateApk.validate(context, apk, manifest)
            } else {
                message("Downloading ${manifest.versionName}…")
                partial.outputStream().use { kotlin.check(UpdateHttp.read(manifest.apkUrl, it, 150L * 1024 * 1024) == 200) { "Update download failed." } }
                coroutineContext.ensureActive()
                if (automatic && !enabled()) return@withContext true
                UpdateApk.validate(context, partial, manifest)
                coroutineContext.ensureActive()
                if (automatic && !enabled()) return@withContext true
                // No installer can read a partially downloaded file; installation uses the same mutex.
                kotlin.check(partial.renameTo(apk)) { "Could not save the update." }
                manifestFile.writeText(text)
            }
            coroutineContext.ensureActive()
            if (automatic && !enabled()) return@withContext true
            message("${manifest.versionName} is ready to install.", true)
            if (prefs.getLong("notified_code", 0) != manifest.versionCode && UpdateNotifications.ready(context))
                prefs.edit().putLong("notified_code", manifest.versionCode).apply()
            true
        } catch (cancelled: CancellationException) {
            message("Update check paused. You can check again in Settings.")
            throw cancelled
        } catch (error: Exception) {
            message("Update could not be prepared. Check your connection or the release files, then retry.")
            false
        } finally {
            partial.delete()
            prefs.edit().putLong("checked", System.currentTimeMillis()).apply()
            mutable.value = mutable.value.copy(busy = false)
            lock.unlock()
        }
    }
    suspend fun install(activity: Context) {
        if (!lock.tryLock()) return
        try {
            withContext(Dispatchers.IO) {
                UpdateApk.validate(context, apk, UpdateManifest.parse(manifestFile.readText()))
            }
            if (Build.VERSION.SDK_INT >= 26 && !context.packageManager.canRequestPackageInstalls()) {
                activity.startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}")))
                message("Allow Sung to install updates, return here, then tap Install again.")
            } else {
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.updates", apk)
                activity.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/vnd.android.package-archive")
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
            }
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) {
            apk.delete(); manifestFile.delete(); UpdateNotifications.cancel(context)
            message("This update cannot be installed. Check for updates to download a verified copy.", false)
        } finally { lock.unlock() }
    }
    companion object {
        @Volatile private var instance: AppUpdates? = null
        fun get(context: Context): AppUpdates = instance ?: synchronized(this) {
            instance ?: AppUpdates(context.applicationContext).also { instance = it }
        }
    }
}
data class UpdateState(val enabled: Boolean = true, val busy: Boolean = false, val ready: Boolean = false, val message: String = "")
