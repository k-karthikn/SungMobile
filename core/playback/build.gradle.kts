plugins { alias(libs.plugins.android.library) }

android {
    namespace = "com.karthi.sungmobile.core.playback"
    compileSdk { version = release(37) }
    defaultConfig { minSdk = 26 }
    testOptions.unitTests.isIncludeAndroidResources = true
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(project(":core:model"))
    implementation(libs.media3.exoplayer)
    api(libs.media3.session)
    implementation(libs.coroutines.android)
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.coroutines.test)
}
