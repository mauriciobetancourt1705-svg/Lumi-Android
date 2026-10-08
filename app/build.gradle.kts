plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.lumi.android"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.lumi.android"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.2.0-agent-stack"
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
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    // TTS neural local, gratuito y reemplazable; modelo se descarga/instala aparte.
    implementation("dev.ffmpegkit-maintained:kokoro-android:0.1.0")
    // Wake word local basado en openWakeWord + ONNX Runtime.
    implementation("com.github.msnilsen:openwakeword-android:0.1.0")
}
