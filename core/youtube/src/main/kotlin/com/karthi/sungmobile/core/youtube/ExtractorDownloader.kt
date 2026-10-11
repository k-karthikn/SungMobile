package com.karthi.sungmobile.core.youtube

import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import java.io.IOException
import java.util.concurrent.TimeUnit

internal const val USER_AGENT = "Mozilla/5.0 (Linux; Android 15) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36"

internal class HttpStatusException(val status: Int) : IOException("HTTP $status")

internal class ExtractorDownloader(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .callTimeout(40, TimeUnit.SECONDS)
        .followSslRedirects(false)
        .build(),
    private val maxBodyBytes: Long = 16L * 1024 * 1024,
) : Downloader() {
    override fun execute(request: Request): Response {
        val method = request.httpMethod()
        val data = request.dataToSend()
        val body = data?.toRequestBody() ?: if (method in setOf("POST", "PUT", "PATCH")) {
            ByteArray(0).toRequestBody()
        } else null
        val builder = okhttp3.Request.Builder().url(request.url()).method(method, body)
            .header("User-Agent", USER_AGENT)
        request.headers().forEach { (name, values) ->
            builder.removeHeader(name)
            values.forEach { builder.addHeader(name, it) }
        }
        val call = client.newCall(builder.build())
        val operation = HttpOperation.current.get()
        operation?.attach(call)
        try {
            call.execute().use { response ->
                if (response.code == 429) throw HttpStatusException(429)
                val responseBody = response.body
                if (responseBody != null && (responseBody.contentLength() > maxBodyBytes ||
                        responseBody.source().request(maxBodyBytes + 1))) {
                    throw IOException("Catalog response exceeds size limit")
                }
                return Response(response.code, response.message, response.headers.toMultimap(),
                    responseBody?.string().orEmpty(), response.request.url.toString())
            }
        } finally {
            operation?.detach(call)
        }
    }
}
