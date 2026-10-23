plugins { alias(libs.plugins.kotlin.jvm) }

kotlin { compilerOptions { jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11 } }
java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

dependencies {
    implementation(project(":core:model"))
    implementation(libs.newpipe)
    implementation(libs.okhttp)
    implementation("com.google.code.gson:gson:2.11.0")
    implementation(libs.coroutines.core)
    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.mockwebserver)
}

tasks.withType<Test>().configureEach {
    systemProperty("sung.live", providers.gradleProperty("live").getOrElse("false"))
    testLogging { events("passed", "skipped", "failed") }
}
