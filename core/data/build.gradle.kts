plugins { alias(libs.plugins.android.library) }
android {
    namespace = "com.karthi.sungmobile.core.data"
    compileSdk { version = release(37) }
    defaultConfig {
        minSdk = 26
        javaCompileOptions.annotationProcessorOptions.arguments["room.schemaLocation"] = "$projectDir/schemas"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    testOptions.unitTests.isIncludeAndroidResources = true
}
dependencies {
    implementation(project(":core:model"))
    implementation(libs.room.runtime)
    annotationProcessor(libs.room.compiler)
    implementation(libs.datastore.preferences)
    implementation(libs.coroutines.android)
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.coroutines.test)
}
