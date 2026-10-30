package com.karthi.sungmobile.updates

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import com.karthi.sungmobile.feature.feedback.DiagnosticLog
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/** One updater per process. Files and preferences are private; installation is always user initiated. */
class AppUpdates internal constructor(private val context: Context) {
    private val prefs = context.getSharedPreferences("app_updates", Context.MODE_PRIVATE)
    private val directory = File(context.filesDir, "updates").apply { mkdirs() }
    private val apk = File(directory, "update.apk")
    private val manifestFile = File(directory, "update.json")
    private val partial = File(directory, "update.part")
    private val partialManifest = File(directory, "update.json.part")
    private val lock = Mutex()
    private val startupScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val initialMessage = prefs.getString("message", "Updates have not been checked yet.").orEmpty()
        .let { if (it.endsWith("is ready to install.") && !(apk.exists() && manifestFile.exists()))
            "Updates have not been checked yet." else it }
    private val mutable = MutableStateFlow(UpdateState(enabled = enabled(),
        ready = false, message = if (apk.exists() && manifestFile.exists()) "Checking saved update…" else initialMessage))
    val state = mutable.asStateFlow()
    fun enabled() = prefs.getBoolean("enabled", true)
    fun setEnabled(value: Boolean) {
        prefs.edit().putBoolean("enabled", value).apply()
        mutable.value = mutable.value.copy(enabled = value)
        UpdateSchedule.configure(context, value)
        if (value) UpdateSchedule.checkSoon(context)
    }
    fun start(): Job {
        mutable.value = mutable.value.copy(busy = true)
        return startupScope.launch {
            try {
                lock.withLock { validateSavedUpdate() }
            } finally {
                mutable.value = mutable.value.copy(busy = false)
            }
            UpdateSchedule.configure(context, enabled())
            if (enabled() && System.currentTimeMillis() - prefs.getLong("checked", 0) > 12 * 60 * 60 * 1000L)
                UpdateSchedule.checkSoon(context)
        }
    }

    private fun validateSavedUpdate() {
        // A process may have stopped while downloading or while committing the pair.
        partial.delete()
        partialManifest.delete()
        if (!apk.exists() && !manifestFile.exists()) {
            if (prefs.getString("message", "").orEmpty().endsWith("is ready to install.")) {
                UpdateNotifications.cancel(context)
                message("Updates have not been checked yet.", false)
            }
            return
        }
        if (!apk.isFile || !manifestFile.isFile) {
            discardSavedUpdate("The saved update is incomplete. Check again to download a fresh copy.")
            return
        }
        try {
            val manifest = UpdateManifest.parse(manifestFile.readText())
            if (!manifest.newerThan(UpdateApk.code(UpdateApk.installed(context)), context.packageName)) {
                discardSavedUpdate("The downloaded update is already installed.")
                return
            }
            UpdateApk.validate(context, apk, manifest)
            message("${manifest.versionName} is ready to install.", true)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            DiagnosticLog.recordError("saved_update_invalid", error)
            discardSavedUpdate("The saved update could not be verified. Check again to download a fresh copy.")
        }
    }

    private fun discardSavedUpdate(text: String) {
        apk.delete()
        manifestFile.delete()
        partial.delete()
        partialManifest.delete()
        UpdateNotifications.cancel(context)
        message(text, false)
    }

    private fun commitDownloadedUpdate(text: String, manifest: UpdateManifest) {
        writeAndSync(partialManifest, text)
        // Both temporary files are complete and verified before either saved file changes.
        // The manifest is the commit marker and is atomically replaced last.
        Files.move(partial.toPath(), apk.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        Files.move(partialManifest.toPath(), manifestFile.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        UpdateApk.validate(context, apk, manifest)
    }

    private fun writeAndSync(file: File, text: String) {
        FileOutputStream(file).use { output ->
            output.write(text.toByteArray(Charsets.UTF_8))
            output.fd.sync()
        }
    }
    private fun message(text: String, ready: Boolean = mutable.value.ready) {
        prefs.edit().putString("message", text).apply()
        mutable.value = mutable.value.copy(message = text, ready = ready)
    }
    suspend fun check(automatic: Boolean): Boolean = withContext(Dispatchers.IO) {
        if (!lock.tryLock()) return@withContext true
        mutable.value = mutable.value.copy(busy = true)
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
            val cachedIsValid = apk.isFile && manifestFile.isFile && manifestFile.readText() == text &&
                runCatching { UpdateApk.validate(context, apk, manifest) }.isSuccess
            if (!cachedIsValid) {
                message("Downloading ${manifest.versionName}…")
                partial.outputStream().use { kotlin.check(UpdateHttp.read(manifest.apkUrl, it, 150L * 1024 * 1024) == 200) { "Update download failed." } }
                coroutineContext.ensureActive()
                if (automatic && !enabled()) return@withContext true
                UpdateApk.validate(context, partial, manifest)
                coroutineContext.ensureActive()
                if (automatic && !enabled()) return@withContext true
                mutable.value = mutable.value.copy(ready = false)
                commitDownloadedUpdate(text, manifest)
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
            DiagnosticLog.recordError("update_failed", error)
            message("Update could not be prepared. Check your connection or the release files, then retry.")
            false
        } finally {
            partial.delete()
            partialManifest.delete()
            prefs.edit().putLong("checked", System.currentTimeMillis()).apply()
            mutable.value = mutable.value.copy(busy = false)
            lock.unlock()
        }
    }
    suspend fun install(activity: Context) {
        if (!lock.tryLock()) return
        try {
            withContext(Dispatchers.IO) {
                require(apk.isFile && manifestFile.isFile) { "No verified update is saved" }
                UpdateApk.validate(context, apk, UpdateManifest.parse(manifestFile.readText()))
            }
            if (!context.packageManager.canRequestPackageInstalls()) {
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
        // get() stores only applicationContext, so this process-wide singleton cannot retain an Activity.
        @field:SuppressLint("StaticFieldLeak")
        @Volatile private var instance: AppUpdates? = null
        fun get(context: Context): AppUpdates = instance ?: synchronized(this) {
            instance ?: AppUpdates(context.applicationContext).also { instance = it }
        }
    }
}
data class UpdateState(val enabled: Boolean = true, val busy: Boolean = false, val ready: Boolean = false, val message: String = "")
