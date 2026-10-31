package com.karthi.sungmobile.core.data.spotify

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.TimeUnit

/** Acquires Spotify's browser client token used by its web-player library queries. */
internal class SpotifyClientTokenProvider {
    private val mutex = Mutex()
    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .callTimeout(20, TimeUnit.SECONDS)
        .build()
    private val deviceId = UUID.randomUUID().toString()
    @Volatile private var cachedToken: String? = null
    @Volatile private var expiresAtMs: Long = 0

    suspend fun get(): String? = mutex.withLock {
        withContext(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            cachedToken?.takeIf { now < expiresAtMs - TOKEN_EXPIRY_GRACE_MS }?.let { return@withContext it }
            runCatching {
                val body = JSONObject().put("client_data", JSONObject()
                    .put("client_version", SPOTIFY_APP_VERSION)
                    .put("client_id", SPOTIFY_CLIENT_ID)
                    .put("js_sdk_data", JSONObject()
                        .put("device_brand", "Apple")
                        .put("device_model", "unknown")
                        .put("os", "macos")
                        .put("os_version", "10.15.7")
                        .put("device_id", deviceId)
                        .put("device_type", "computer")))
                    .toString()
                val request = Request.Builder()
                    .url(CLIENT_TOKEN_URL)
                    .header("User-Agent", CLIENT_TOKEN_USER_AGENT)
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .post(body.toRequestBody(JSON_MEDIA_TYPE))
                    .build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@use null
                    val granted = JSONObject(response.body?.string().orEmpty())
                        .optJSONObject("granted_token") ?: return@use null
                    val token = granted.optString("token").takeIf(String::isNotBlank) ?: return@use null
                    val ttlSeconds = granted.optLong("expires_after_seconds", DEFAULT_TTL_SECONDS)
                        .coerceIn(60, MAX_TTL_SECONDS)
                    cachedToken = token
                    expiresAtMs = System.currentTimeMillis() + ttlSeconds * 1_000L
                    token
                }
            }.getOrNull() ?: cachedToken?.takeIf { System.currentTimeMillis() < expiresAtMs }
        }
    }

    private companion object {
        const val CLIENT_TOKEN_URL = "https://clienttoken.spotify.com/v1/clienttoken"
        const val SPOTIFY_APP_VERSION = "1.2.62.476.g2ad6e7f3"
        const val SPOTIFY_CLIENT_ID = "d8a5ed958d274c2e8ee717e6a4b0971d"
        const val CLIENT_TOKEN_USER_AGENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/135.0.0.0 Safari/537.36"
        const val TOKEN_EXPIRY_GRACE_MS = 60_000L
        const val DEFAULT_TTL_SECONDS = 7_200L
        const val MAX_TTL_SECONDS = 86_400L
        val JSON_MEDIA_TYPE = "application/json".toMediaType()
    }
}
