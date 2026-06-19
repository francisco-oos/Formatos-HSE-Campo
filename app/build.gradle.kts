plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.sinopec.formatoshsecampo"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.sinopec.formatoshsecampo"
        minSdk = 23
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.1"

        buildConfigField("String", "DATA_KEY", "\"CAMBIA_ESTA_CLAVE_SUPERVISION_SEGURA_V1.1.1\"")
        buildConfigField("String", "LOCAL_EXPIRES_AT", "\"2026-05-25\"")
        buildConfigField("String", "VERSION_CONTROL_URL", "\"\"")
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlinOptions {
        jvmTarget = "21"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("com.google.zxing:core:3.5.3")
}