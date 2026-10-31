package com.karthi.sungmobile.core.data.spotify

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Request
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.TimeUnit
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

enum class SpotifySessionValidation { VALID, INVALID, UNAVAILABLE }

/**
 * Validates the session cookie using Spotify's current web-player token exchange.
 * Adapted from LastWave's Spotify account connector (GPL-3.0), so this private web-player
 * flow can change without notice. See the app's third-party notices for attribution.
 */
class SpotifySessionValidator(private val sessions: SpotifySessionStore) {
    private val validationMutex = Mutex()
    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .callTimeout(30, TimeUnit.SECONDS)
        .build()

    @Volatile private var cachedSecret: PlayerSecret? = null
    @Volatile private var secretLoadedAtMs = 0L

    suspend fun validate(): SpotifySessionValidation = validationMutex.withLock {
        withContext(Dispatchers.IO) {
            val cookie = sessions.readSessionCookie() ?: return@withContext SpotifySessionValidation.INVALID
            val cached = sessions.readPersonalToken()
            if (cached != null && cached.expiresAtMs > System.currentTimeMillis() + TOKEN_EXPIRY_MARGIN_MS) {
                return@withContext SpotifySessionValidation.VALID
            }

            val secret = readPlayerSecret() ?: return@withContext SpotifySessionValidation.UNAVAILABLE
            val signingKey = decodeSigningKey(secret.value)
            if (signingKey.isEmpty()) return@withContext SpotifySessionValidation.UNAVAILABLE
            val serverTime = serverTimeSeconds(cookie) ?: (System.currentTimeMillis() / 1_000L)
            val clientTime = System.currentTimeMillis() / 1_000L
            val clientOtp = oneTimePassword(signingKey, clientTime)
            val serverOtp = oneTimePassword(signingKey, serverTime)

            val transport = requestToken(cookie, secret.version, clientOtp, serverOtp, REASON_TRANSPORT)
            val init = if (transport.token?.let(::isUsable) == true) null
                else requestToken(cookie, secret.version, clientOtp, serverOtp, REASON_INIT)
            val token = listOfNotNull(transport, init).firstNotNullOfOrNull { it.token?.takeIf(::isUsable) }
            if (token != null) {
                sessions.savePersonalToken(token.value, token.expiresAtMs)
                return@withContext SpotifySessionValidation.VALID
            }

            val attempts = listOfNotNull(transport, init)
            val rejected = attempts.any { it.status == 401 || it.anonymous }
            if (rejected) {
                sessions.clearSession()
                SpotifySessionValidation.INVALID
            } else {
                SpotifySessionValidation.UNAVAILABLE
            }
        }
    }

    private fun readPlayerSecret(): PlayerSecret? {
        val cached = cachedSecret
        if (cached != null && System.currentTimeMillis() - secretLoadedAtMs < SECRET_CACHE_TTL_MS) return cached
        val html = getText(PLAYER_PAGE) ?: return null
        val bundles = PLAYER_SCRIPT.findAll(html).map { it.value }.distinct().take(MAX_PLAYER_BUNDLES).toList()
        val found = bundles.asSequence().mapNotNull { getText(it, PLAYER_PAGE) }
            .flatMap { parsePlayerSecrets(it).asSequence() }
            .maxByOrNull(PlayerSecret::version)
            ?: readPublishedPlayerSecret()
        if (found != null) {
            cachedSecret = found
            secretLoadedAtMs = System.currentTimeMillis()
        }
        return found
    }

    /** LastWave's published fallback, used when Spotify's page bundle cannot be read. */
    private fun readPublishedPlayerSecret(): PlayerSecret? {
        val body = getText(PUBLISHED_SECRET_URL) ?: return null
        return runCatching {
            val secrets = JSONObject(body)
            val version = secrets.keys().asSequence()
                .mapNotNull(String::toIntOrNull)
                .maxOrNull() ?: return null
            val cipher = secrets.optJSONArray(version.toString()) ?: return null
            if (cipher.length() == 0 || cipher.length() > MAX_SECRET_LENGTH) return null
            val chars = CharArray(cipher.length())
            for (index in chars.indices) {
                val value = cipher.optInt(index, -1)
                if (value !in 0..Char.MAX_VALUE.code) return null
                chars[index] = value.toChar()
            }
            PlayerSecret(String(chars), version)
        }.getOrNull()
    }

    private fun parsePlayerSecrets(script: String): List<PlayerSecret> = SECRET_ENTRY.findAll(script).mapNotNull { match ->
        val raw = match.groupValues[1].ifEmpty { match.groupValues[2] }
        val version = match.groupValues[3].toIntOrNull() ?: return@mapNotNull null
        raw.takeIf { it.isNotBlank() }?.let { PlayerSecret(unescape(it), version) }
    }.toList()

    private fun serverTimeSeconds(cookie: String): Long? {
        val request = Request.Builder().url(SERVER_TIME_URL).headers(webPlayerHeaders(cookie)).get().build()
        return runCatching {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) null
                else JSONObject(response.body?.string().orEmpty()).optLong("serverTime").takeIf { it > 0 }
            }
        }.getOrNull()
    }

    private fun requestToken(cookie: String, version: Int, clientOtp: String, serverOtp: String,
        reason: String): TokenAttempt {
        val url = SESSION_TOKEN_URL.toHttpUrlOrNull()?.newBuilder()
            ?.addQueryParameter("reason", reason)
            ?.addQueryParameter("productType", "web-player")
            ?.addQueryParameter("totp", clientOtp)
            ?.addQueryParameter("totpServer", serverOtp)
            ?.addQueryParameter("totpVer", version.toString())
            ?.build() ?: return TokenAttempt(0, null)
        val request = Request.Builder().url(url).headers(webPlayerHeaders(cookie)).get().build()
        return runCatching {
            client.newCall(request).execute().use { response ->
                val json = if (response.isSuccessful) {
                    runCatching { JSONObject(response.body?.string().orEmpty()) }.getOrNull()?.let { json ->
                        val accessToken = json.optString("accessToken").takeIf { it.isNotBlank() && it != "null" }
                        val expiry = json.optLong("accessTokenExpirationTimestampMs")
                            .takeIf { it > 0 } ?: (System.currentTimeMillis() + DEFAULT_TOKEN_TTL_MS)
                        val anonymous = json.optBoolean("isAnonymous", false)
                        TokenAttempt(response.code,
                            if (anonymous) null else accessToken?.let { SpotifyPersonalToken(it, expiry) }, anonymous)
                    }
                } else null
                json ?: TokenAttempt(response.code, null)
            }
        }.getOrDefault(TokenAttempt(0, null))
    }

    private fun webPlayerHeaders(cookie: String) = okhttp3.Headers.Builder()
        .add("User-Agent", USER_AGENT)
        .add("Accept", "application/json")
        .add("Cookie", "sp_dc=$cookie")
        .add("App-platform", "WebPlayer")
        .add("Spotify-App-Version", SPOTIFY_APP_VERSION)
        .add("Origin", SPOTIFY_ORIGIN)
        .add("Referer", "$SPOTIFY_ORIGIN/")
        .build()

    private fun getText(url: String, referer: String? = null): String? = runCatching {
        val request = Request.Builder().url(url).header("User-Agent", USER_AGENT)
            .apply { referer?.let { header("Referer", it) } }.get().build()
        client.newCall(request).execute().use { response ->
            val body = response.body ?: return@use null
            if (!response.isSuccessful || body.contentLength() > MAX_RESPONSE_BYTES) return@use null
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(8 * 1024)
            val input = body.byteStream()
            var total = 0L
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                total += read
                if (total > MAX_RESPONSE_BYTES) return@use null
                output.write(buffer, 0, read)
            }
            String(output.toByteArray(), Charsets.UTF_8)
        }
    }.getOrNull()

    private fun decodeSigningKey(value: String): ByteArray {
        val digits = buildString {
            value.forEachIndexed { index, character -> append(character.code xor ((index % 33) + 9)) }
        }
        return digits.toByteArray(Charsets.US_ASCII)
    }

    private fun oneTimePassword(secret: ByteArray, timestampSeconds: Long): String {
        val counter = timestampSeconds / 30L
        val counterBytes = ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN).putLong(counter).array()
        val mac = Mac.getInstance("HmacSHA1").apply { init(SecretKeySpec(secret, "HmacSHA1")) }
        val digest = mac.doFinal(counterBytes)
        val offset = digest.last().toInt() and 0x0F
        val binary = ((digest[offset].toInt() and 0x7F) shl 24) or
            ((digest[offset + 1].toInt() and 0xFF) shl 16) or
            ((digest[offset + 2].toInt() and 0xFF) shl 8) or
            (digest[offset + 3].toInt() and 0xFF)
        return (binary % 1_000_000).toString().padStart(6, '0')
    }

    private fun isUsable(token: SpotifyPersonalToken): Boolean =
        token.value.length >= MIN_TOKEN_LENGTH && token.expiresAtMs > System.currentTimeMillis()

    private fun unescape(value: String): String {
        val output = StringBuilder(value.length)
        var index = 0
        while (index < value.length) {
            val character = value[index]
            if (character == '\\' && index + 1 < value.length) {
                val next = value[index + 1]
                output.append(when (next) { 'n' -> '\n'; 'r' -> '\r'; 't' -> '\t'; else -> next })
                index += 2
            } else {
                output.append(character)
                index++
            }
        }
        return output.toString()
    }

    private data class PlayerSecret(val value: String, val version: Int)
    private data class TokenAttempt(val status: Int, val token: SpotifyPersonalToken?, val anonymous: Boolean = false)

    private companion object {
        private const val PLAYER_PAGE = "https://open.spotify.com/"
        private const val SERVER_TIME_URL = "https://open.spotify.com/api/server-time"
        private const val SESSION_TOKEN_URL = "https://open.spotify.com/api/token"
        private const val PUBLISHED_SECRET_URL = "https://raw.githubusercontent.com/xyloflake/spot-secrets-go/refs/heads/main/secrets/secretDict.json"
        private const val SPOTIFY_ORIGIN = "https://open.spotify.com"
        private const val SPOTIFY_APP_VERSION = "1.2.62.476.g2ad6e7f3"
        private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
        private const val REASON_TRANSPORT = "transport"
        private const val REASON_INIT = "init"
        private const val MIN_TOKEN_LENGTH = 100
        private const val DEFAULT_TOKEN_TTL_MS = 3_600_000L
        private const val TOKEN_EXPIRY_MARGIN_MS = 60_000L
        private const val SECRET_CACHE_TTL_MS = 6 * 60 * 60 * 1000L
        private const val MAX_PLAYER_BUNDLES = 4
        private const val MAX_SECRET_LENGTH = 256
        private const val MAX_RESPONSE_BYTES = 12L * 1024 * 1024
        private val PLAYER_SCRIPT = Regex("""https://open\.spotifycdn\.com/cdn/build/web-player/[A-Za-z0-9._~-]+\.(?:js|mjs)""")
        private val SECRET_ENTRY = Regex("""secret:\s*(?:'((?:[^'\\]|\\.)*)'|"((?:[^"\\]|\\.)*)")\s*,\s*version:\s*(\d+)""")
    }
}
