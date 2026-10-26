package com.karthi.sungmobile.feature.feedback

import android.content.Context
import android.os.Build
import androidx.core.content.pm.PackageInfoCompat
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Small app-owned diagnostic log. Exception messages, URLs, titles and user data are never copied. */
object DiagnosticLog {
    private const val MAX_BYTES = 128 * 1024
    private var file: File? = null
    private val lock = Any()

    fun install(context: Context) {
        synchronized(lock) {
            if (file != null) return
            file = File(context.filesDir, "diagnostics/events.log").also { it.parentFile?.mkdirs() }
            val previous = Thread.getDefaultUncaughtExceptionHandler()
            Thread.setDefaultUncaughtExceptionHandler { thread, error ->
                recordError("crash", error)
                previous?.uncaughtException(thread, error)
            }
        }
        recordEvent("app_started")
    }

    fun recordEvent(event: String) = append(event.takeIf { it.matches(Regex("[a-z_]{1,40}")) } ?: "event")

    fun recordError(event: String, error: Throwable) {
        val frames = error.stackTrace.asSequence().filter { it.className.startsWith("com.karthi.sungmobile") }
            .take(6).joinToString(" | ") { "${it.className}.${it.methodName}:${it.lineNumber}" }
        val type = error.javaClass.simpleName.take(80)
        val cause = error.cause?.javaClass?.simpleName?.take(80).orEmpty()
        append("${event.take(40)} $type cause=$cause at $frames")
    }

    private fun append(message: String) = synchronized(lock) {
        runCatching {
            val target = file ?: return@synchronized
            val stamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z", Locale.ROOT).format(Date())
            if (target.length() > MAX_BYTES) {
                val tail = target.readText().takeLast(MAX_BYTES / 2).substringAfter('\n', "")
                target.writeText(tail)
            }
            target.appendText("$stamp $message\n")
        }
        Unit
    }

    fun export(context: Context): File = synchronized(lock) {
        val directory = File(context.cacheDir, "diagnostics").apply { mkdirs() }
        directory.listFiles()?.sortedByDescending(File::lastModified)?.drop(3)?.forEach(File::delete)
        val report = File(directory, "sungmobile-${System.currentTimeMillis()}.txt")
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        report.writeText(buildString {
            appendLine("SungMobile ${packageInfo.versionName} (${PackageInfoCompat.getLongVersionCode(packageInfo)})")
            appendLine("Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            appendLine("App errors and crash locations only; no logcat, URLs, song titles or listening history.")
            appendLine()
            append(file?.takeIf(File::isFile)?.readText()?.takeLast(MAX_BYTES).orEmpty())
        })
        report
    }
}
