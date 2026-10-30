package com.karthi.sungmobile.updates

import android.app.job.JobScheduler
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.Signature
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlinx.coroutines.runBlocking
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [27])
class UpdateSafetyTest {
    private val pkg = "com.karthi.sungmobile.stage0"
    private val manifest = UpdateManifest(21, "0.12.1", pkg, "${RELEASE_ROOT}v0.12.1/Sung.apk", "a".repeat(64))
    private fun json(url: String = manifest.apkUrl, code: Long = 21, hash: String = manifest.sha256) =
        """{"versionCode":$code,"versionName":"0.12.1","packageName":"$pkg","apkUrl":"$url","sha256":"$hash"}"""
    @Test fun codesNotNamesDecideUpdates() {
        assertTrue(manifest.newerThan(20, pkg))
        assertFalse(manifest.newerThan(21, pkg))
        assertFalse(manifest.newerThan(22, pkg))
        assertFalse(manifest.newerThan(20, "other.package"))
        assertFalse(manifest.copy(versionName = "999.0", versionCode = 1).newerThan(20, pkg))
    }
    @Test fun metadataRequiresChecksumAndValidVersion() {
        assertEquals(manifest, UpdateManifest.parse(json()))
        assertThrows(IllegalArgumentException::class.java) { UpdateManifest.parse(json(hash = "bad")) }
        assertThrows(IllegalArgumentException::class.java) { UpdateManifest.parse(json(code = 0)) }
        assertThrows(IllegalArgumentException::class.java) { UpdateManifest.parse(json(code = 2_100_000_001)) }
    }
    @Test fun assetsCannotEscapeRepository() {
        listOf("http://github.com/k-karthikn/SungMobile/releases/download/a/b.apk",
            "https://github.com/other/repo/releases/download/a/b.apk",
            "https://github.com.evil.test/k-karthikn/SungMobile/releases/download/a/b.apk",
            "${RELEASE_ROOT}../../evil.apk", "${manifest.apkUrl}?redirect=evil").forEach {
            assertFalse(it, isReleaseAsset(it))
        }
    }
    @Suppress("DEPRECATION")
    private fun info(code: Int, signer: String = "01020304", name: String = pkg, minSdk: Int = 26) = PackageInfo().apply {
        packageName = name; versionCode = code
        signatures = arrayOf(Signature(signer))
        applicationInfo = ApplicationInfo().apply { minSdkVersion = minSdk }
    }
    @Test fun identityRejectsWrongKeyPackageCodeAndAndroidVersion() {
        UpdateApk.validateIdentity(info(20), info(21), manifest, pkg, 27)
        listOf(info(21, signer = "05060708"), info(21, name = "other"), info(20), info(22), info(21, minSdk = 28)).forEach {
            assertThrows(IllegalArgumentException::class.java) { UpdateApk.validateIdentity(info(20), it, manifest, pkg, 27) }
        }
        assertThrows(IllegalArgumentException::class.java) { UpdateApk.validateIdentity(info(21), info(21), manifest, pkg, 27) }
    }
    @Test fun corruptDownloadRejectedBeforeArchiveParsing() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val file = java.io.File(context.cacheDir, "corrupt.apk").apply { writeText("not an APK") }
        assertThrows(IllegalArgumentException::class.java) { UpdateApk.validate(context, file, manifest) }
        file.delete()
    }
    @Test fun enabledByDefaultTogglePersistsAndCancelsScheduledJobs() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("app_updates", 0).edit().clear().commit()
        val updater = AppUpdates(context)
        assertTrue(updater.enabled())
        updater.setEnabled(true)
        val scheduler = context.getSystemService(JobScheduler::class.java)
        val periodic = scheduler.allPendingJobs.single { it.isPeriodic }
        assertTrue(periodic.isPersisted)
        assertEquals(android.app.job.JobInfo.NETWORK_TYPE_UNMETERED, periodic.networkType)
        updater.setEnabled(false)
        assertTrue(scheduler.allPendingJobs.isEmpty())
        assertFalse(AppUpdates(context).enabled())
    }

    @Test fun startupClearsCorruptSavedPairAndAbandonedStagingFiles() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val updates = File(context.filesDir, "updates").apply { deleteRecursively(); mkdirs() }
        context.getSharedPreferences("app_updates", 0).edit().clear().putBoolean("enabled", false).commit()
        val apk = File(updates, "update.apk").apply { writeText("truncated apk") }
        val manifestFile = File(updates, "update.json").apply { writeText("{") }
        val partial = File(updates, "update.part").apply { writeText("abandoned download") }
        val partialManifest = File(updates, "update.json.part").apply { writeText("abandoned metadata") }

        val updater = AppUpdates(context)
        updater.start().join()

        assertFalse(updater.state.value.ready)
        assertTrue(updater.state.value.message.contains("could not be verified"))
        assertFalse(apk.exists())
        assertFalse(manifestFile.exists())
        assertFalse(partial.exists())
        assertFalse(partialManifest.exists())
    }

    @Test fun startupRejectsTruncatedApkAgainstAValidManifest() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val updates = File(context.filesDir, "updates").apply { deleteRecursively(); mkdirs() }
        context.getSharedPreferences("app_updates", 0).edit().clear().putBoolean("enabled", false).commit()
        val code = UpdateApk.code(UpdateApk.installed(context)) + 1
        File(updates, "update.apk").writeText("not an apk")
        File(updates, "update.json").writeText(json(code = code))

        val updater = AppUpdates(context)
        updater.start().join()

        assertFalse(updater.state.value.ready)
        assertTrue(updater.state.value.message.contains("could not be verified"))
        assertFalse(File(updates, "update.apk").exists())
        assertFalse(File(updates, "update.json").exists())
    }

    @Test fun startupDropsAnUpdateThatIsAlreadyInstalled() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val updates = File(context.filesDir, "updates").apply { deleteRecursively(); mkdirs() }
        context.getSharedPreferences("app_updates", 0).edit().clear().putBoolean("enabled", false).commit()
        val installedCode = UpdateApk.code(UpdateApk.installed(context))
        File(updates, "update.apk").writeText("no longer relevant")
        File(updates, "update.json").writeText(json(code = installedCode))

        val updater = AppUpdates(context)
        updater.start().join()

        assertFalse(updater.state.value.ready)
        assertTrue(updater.state.value.message.contains("already installed"))
        assertFalse(File(updates, "update.apk").exists())
        assertFalse(File(updates, "update.json").exists())
    }
}
