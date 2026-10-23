package com.karthi.sungmobile.updates

import org.json.JSONObject
import java.net.URI

internal const val RELEASE_ROOT = "https://github.com/k-karthikn/SungMobile/releases/download/"
internal data class UpdateManifest(val versionCode: Long, val versionName: String, val packageName: String,
    val apkUrl: String, val sha256: String) {
    fun newerThan(installed: Long, installedPackage: String) = packageName == installedPackage && versionCode > installed
    companion object {
        fun parse(text: String): UpdateManifest {
            val json = JSONObject(text)
            return UpdateManifest(json.getLong("versionCode"), json.getString("versionName"),
                json.getString("packageName"), json.getString("apkUrl"), json.getString("sha256").lowercase()).also {
                require(it.versionCode in 1..2_100_000_000L)
                require(it.versionName.length in 1..100)
                require(it.sha256.matches(Regex("[a-f0-9]{64}")))
                require(isReleaseAsset(it.apkUrl))
            }
        }
    }
}
internal fun isReleaseAsset(url: String): Boolean = runCatching {
    val uri = URI(url)
    url.startsWith(RELEASE_ROOT) && uri.host == "github.com" && uri.userInfo == null &&
        uri.scheme == "https" && uri.port == -1 && uri.fragment == null && uri.query == null &&
        !uri.path.split('/').any { it == ".." || it == "." }
}.getOrDefault(false)
