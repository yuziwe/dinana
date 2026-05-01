plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
}

import java.util.Properties
import java.util.concurrent.TimeUnit

// Load keystore.properties for local release builds
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(keystorePropertiesFile.inputStream())
}

fun runCommand(command: String, dir: File): String = try {
    val proc = ProcessBuilder(command.split("\\s".toRegex()))
        .directory(dir)
        .redirectOutput(ProcessBuilder.Redirect.PIPE)
        .redirectError(ProcessBuilder.Redirect.PIPE)
        .start()
    if (proc.waitFor(2, TimeUnit.SECONDS)) {
        proc.inputStream.bufferedReader().readText().trim()
    } else {
        proc.destroyForcibly()
        ""
    }
} catch (_: Exception) { "" }

// Derive version from Git tag (e.g. "v1.0.2" -> "1.0.2"), fallback to "1.0.0"
val appVersion = runCommand("git tag --points-at HEAD", rootProject.projectDir)
    .removePrefix("v").ifBlank { "1.0.0" }
val gitSha = runCommand("git rev-parse --short HEAD", rootProject.projectDir)

// Derive versionCode from semver (e.g. "1.0.2" -> 10002, "2.3.4" -> 20304)
val versionParts = appVersion.split(".").map { it.toIntOrNull() ?: 0 }
val appVersionCode = versionParts.getOrElse(0) { 1 } * 10000 +
    versionParts.getOrElse(1) { 0 } * 100 +
    versionParts.getOrElse(2) { 0 }

// Signing config: try local properties first, then CI env vars
val ksFile = keystoreProperties.getProperty("storeFile") ?: System.getenv("KEYSTORE_PATH")
val ksPassword = keystoreProperties.getProperty("storePassword") ?: System.getenv("KEYSTORE_PASSWORD")
val ksKeyAlias = keystoreProperties.getProperty("keyAlias") ?: System.getenv("KEY_ALIAS")
val ksKeyPassword = keystoreProperties.getProperty("keyPassword") ?: System.getenv("KEY_PASSWORD")

val hasReleaseSigning = ksFile != null && ksPassword != null && ksKeyAlias != null && ksKeyPassword != null

android {
    namespace = "com.dinana.blog"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.dinana.blog"
        minSdk = 26
        targetSdk = 34
        versionCode = appVersionCode
        versionName = appVersion

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        create("release") {
            if (hasReleaseSigning) {
                storeFile = rootProject.file(ksFile!!)
                storePassword = ksPassword
                keyAlias = ksKeyAlias
                keyPassword = ksKeyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.10"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Core
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")

    // Compose BOM
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // OkHttp for GitHub REST API
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Kotlinx Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    // Encrypted SharedPreferences for token storage
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")

    // Markdown rendering (Markwon)
    implementation("io.noties.markwon:core:4.6.2")
    implementation("io.noties.markwon:image-glide:4.6.2")
    implementation("com.github.bumptech.glide:glide:4.16.0")
}
