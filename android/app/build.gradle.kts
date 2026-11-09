import java.io.File
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}


// Clave local de desarrollo. Se incorpora al APK: no distribuir este build.
val presageLocalFile = File(
    System.getProperty("user.home"),
    ".config/smart-spectra-challenge/presage.properties"
)
val presageLocalProperties = Properties().apply {
    if (presageLocalFile.isFile) {
        presageLocalFile.inputStream().use { input -> load(input) }
    }
}
val presageLocalKey = presageLocalProperties.getProperty("PRESAGE_API_KEY", "")

android {
    namespace = "com.hormigasais.presagechallenge"
    compileSdk = 36
    compileSdkMinor = 1

    defaultConfig {
        applicationId = "com.hormigasais.presagechallenge"
        minSdk = 28
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
        // Vacío deliberadamente: no colocar una clave privada dentro del APK.
        buildConfigField("String", "PRESAGE_API_KEY", "\"${presageLocalKey.replace("\\", "\\\\").replace("\"", "\\\"")}\"")
}

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.activity:activity-ktx:1.8.1")
    implementation("androidx.core:core-ktx:1.18.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.10.0")
    implementation("androidx.camera:camera-view:1.6.0")
    implementation("com.presagetech:smartspectra:3.4.0")
}
