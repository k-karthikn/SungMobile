package com.karthi.sungmobile.updates

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import java.io.File
import java.security.MessageDigest

internal object UpdateApk {
    @Suppress("DEPRECATION")
    private val flags get() = if (Build.VERSION.SDK_INT >= 28) PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES
    @Suppress("DEPRECATION")
    fun installed(context: Context): PackageInfo = context.packageManager.getPackageInfo(context.packageName, flags)
    @Suppress("DEPRECATION")
    fun code(info: PackageInfo): Long = if (Build.VERSION.SDK_INT >= 28) info.longVersionCode else info.versionCode.toLong()
    @Suppress("DEPRECATION")
    private fun signers(info: PackageInfo): Set<String> = (if (Build.VERSION.SDK_INT >= 28)
        info.signingInfo?.apkContentsSigners else info.signatures).orEmpty().map { signature ->
        MessageDigest.getInstance("SHA-256").digest(signature.toByteArray()).joinToString("") { "%02x".format(it) }
    }.toSet()
    fun validate(context: Context, file: File, manifest: UpdateManifest) {
        val hash = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(32 * 1024)
            while (true) { val count = input.read(buffer); if (count < 0) break; hash.update(buffer, 0, count) }
        }
        require(hash.digest().joinToString("") { "%02x".format(it) } == manifest.sha256) { "Update checksum mismatch" }
        @Suppress("DEPRECATION")
        val archive = context.packageManager.getPackageArchiveInfo(file.path, flags) ?: error("Invalid update APK")
        validateIdentity(installed(context), archive, manifest, context.packageName, Build.VERSION.SDK_INT)
    }
    internal fun validateIdentity(current: PackageInfo, archive: PackageInfo, manifest: UpdateManifest, packageName: String, sdk: Int) {
        require(manifest.newerThan(code(current), packageName)) { "Update is not newer than installed app" }
        require(archive.packageName == packageName && code(archive) == manifest.versionCode) { "Update package/version mismatch" }
        require((archive.applicationInfo?.minSdkVersion ?: Int.MAX_VALUE) <= sdk) { "Update requires a newer Android version" }
        require(signers(current).isNotEmpty() && signers(archive) == signers(current)) { "Update signing key does not match" }
    }
}
