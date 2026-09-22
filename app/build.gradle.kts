import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val localSecrets = Properties()
val secretsFile = rootProject.file("hse-secrets.properties")
if (secretsFile.exists()) {
    secretsFile.inputStream().use(localSecrets::load)
}

fun readLocalValue(key: String, fallback: String): String {
    val env = System.getenv(key)?.trim().orEmpty()
    return if (env.isNotEmpty()) env else localSecrets.getProperty(key, fallback).trim()
}

fun quoted(value: String): String =
    "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

android {
    namespace = "com.sinopec.formatoshsecampo"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.sinopec.formatoshsecampo"
        minSdk = 23
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"

        // Los valores reales se leen de hse-secrets.properties (ignorado por Git)
        // o de variables de entorno. Los fallbacks son deliberadamente genéricos
        // para permitir compilar/probar el proyecto sin publicar secretos.
        buildConfigField(
            "String",
            "DATA_KEY",
            quoted(readLocalValue("HSE_DATA_KEY", "CHANGE_ME_HSE_DATA_KEY"))
        )
        buildConfigField(
            "String",
            "HERMES_BRIDGE_KEY",
            quoted(readLocalValue("HERMES_BRIDGE_KEY", "CHANGE_ME_16BYTE"))
        )
        buildConfigField("String", "LOCAL_EXPIRES_AT", "\"2026-12-31\"")
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
