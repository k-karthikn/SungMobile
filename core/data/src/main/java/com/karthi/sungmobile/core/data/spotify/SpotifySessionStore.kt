package com.karthi.sungmobile.core.data.spotify

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Keeps the Spotify web session cookie encrypted with a key held by Android Keystore. */
class SpotifySessionStore(context: Context) {
    private val file = File(context.applicationContext.noBackupFilesDir, FILE_NAME)
    private val tokenFile = File(context.applicationContext.noBackupFilesDir, TOKEN_FILE_NAME)

    suspend fun hasSession(): Boolean = withContext(Dispatchers.IO) {
        decrypt(file)?.isNotBlank() == true
    }

    suspend fun saveSessionCookie(value: String) = withContext(Dispatchers.IO) {
        val clean = value.trim().removePrefix("sp_dc=").substringBefore(';').trim()
        require(clean.isNotBlank() && clean.length <= MAX_COOKIE_LENGTH && !clean.any(Char::isWhitespace)) {
            "Spotify session cookie is invalid"
        }
        encrypt(file, clean)
        tokenFile.delete()
    }

    suspend fun readSessionCookie(): String? = withContext(Dispatchers.IO) { decrypt(file) }

    suspend fun savePersonalToken(value: String, expiresAtMs: Long) = withContext(Dispatchers.IO) {
        require(value.isNotBlank() && value.length <= MAX_COOKIE_LENGTH && expiresAtMs > 0)
        encrypt(tokenFile, "$expiresAtMs\n$value")
    }

    suspend fun readPersonalToken(): SpotifyPersonalToken? = withContext(Dispatchers.IO) {
        val stored = decrypt(tokenFile) ?: return@withContext null
        val expiry = stored.substringBefore('\n').toLongOrNull() ?: return@withContext null
        val value = stored.substringAfter('\n', "")
        if (value.isBlank()) null else SpotifyPersonalToken(value, expiry)
    }

    suspend fun clearSession() = withContext(Dispatchers.IO) {
        file.delete()
        tokenFile.delete()
    }

    private fun encrypt(target: File, value: String) {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val encrypted = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        val encoded = Base64.encodeToString(cipher.iv + encrypted, Base64.NO_WRAP)
        val temporary = File(target.parentFile, "${target.name}.tmp")
        temporary.writeText(encoded, Charsets.UTF_8)
        Files.move(temporary.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
    }

    private fun decrypt(target: File): String? = runCatching {
        if (!target.isFile) return null
        val payload = Base64.decode(target.readText(Charsets.UTF_8), Base64.NO_WRAP)
        require(payload.size > IV_LENGTH)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(TAG_LENGTH_BITS, payload.copyOfRange(0, IV_LENGTH)))
        String(cipher.doFinal(payload.copyOfRange(IV_LENGTH, payload.size)), Charsets.UTF_8)
    }.getOrNull()

    private fun key(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(KeyGenParameterSpec.Builder(KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true)
            .build())
        return generator.generateKey()
    }

    companion object {
        private const val FILE_NAME = "spotify-session.enc"
        private const val TOKEN_FILE_NAME = "spotify-token.enc"
        private const val KEY_ALIAS = "com.karthi.sungmobile.spotify.session"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val IV_LENGTH = 12
        private const val TAG_LENGTH_BITS = 128
        private const val MAX_COOKIE_LENGTH = 8_192
    }
}

data class SpotifyPersonalToken(val value: String, val expiresAtMs: Long)

/** Returns only Spotify's session cookie value; never return or log the full cookie header. */
fun extractSpotifySessionCookie(cookieHeader: String?): String? = cookieHeader
    ?.split(';')
    ?.asSequence()
    ?.map(String::trim)
    ?.firstOrNull { it.startsWith("sp_dc=", ignoreCase = true) }
    ?.substringAfter('=')
    ?.trim()
    ?.takeIf { it.isNotBlank() && it.length <= 8_192 && !it.any(Char::isWhitespace) }
