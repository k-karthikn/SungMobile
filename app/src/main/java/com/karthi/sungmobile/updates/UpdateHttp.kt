package com.karthi.sungmobile.updates

import kotlinx.coroutines.ensureActive
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URI
import kotlin.coroutines.coroutineContext

/** Bounded HTTPS reads; GitHub asset redirects never carry credentials. */
internal object UpdateHttp {
    suspend fun read(url: String, out: OutputStream, limit: Long): Int {
        var target = url
        repeat(6) {
            coroutineContext.ensureActive()
            val uri = URI(target)
            require(uri.scheme == "https" && uri.userInfo == null && uri.port == -1 && uri.host in
                setOf("api.github.com", "github.com", "release-assets.githubusercontent.com", "objects.githubusercontent.com"))
            val connection = uri.toURL().openConnection() as HttpURLConnection
            try {
                connection.instanceFollowRedirects = false
                connection.connectTimeout = 15_000
                connection.readTimeout = 20_000
                connection.setRequestProperty("User-Agent", "SungMobile-Updater")
                connection.setRequestProperty("Accept", if (uri.host == "api.github.com") "application/vnd.github+json" else "application/octet-stream")
                val status = connection.responseCode
                if (status in listOf(301, 302, 303, 307, 308)) {
                    target = uri.resolve(connection.getHeaderField("Location") ?: error("Missing redirect")).toString()
                } else {
                    if (status != 200) return status
                    require(connection.contentLengthLong <= limit)
                    connection.inputStream.use { input ->
                        val buffer = ByteArray(32 * 1024)
                        var total = 0L
                        while (true) {
                            coroutineContext.ensureActive()
                            val count = input.read(buffer)
                            if (count < 0) break
                            total += count
                            require(total <= limit)
                            out.write(buffer, 0, count)
                        }
                    }
                    return 200
                }
            } finally { connection.disconnect() }
        }
        error("Too many redirects")
    }
    suspend fun json(url: String): Pair<Int, String> {
        val bytes = java.io.ByteArrayOutputStream()
        val status = read(url, bytes, 1_048_576)
        return status to bytes.toString(Charsets.UTF_8.name())
    }
}
