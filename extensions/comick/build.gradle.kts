plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.mangaku.extension.comick"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.mangaku.extension.comick"
        minSdk = 26
        targetSdk = 35
        // Mantener en sincronia con extension.json.
        versionCode = 2
        versionName = "1.0.1"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
}

dependencies {
    // La app anfitriona provee eu.kanade.tachiyomi.*, OkHttp y Gson en runtime.
    compileOnly(project(":core:tachiyomi-api"))
    compileOnly(libs.gson)

    testImplementation(project(":core:tachiyomi-api"))
    testImplementation(libs.gson)
    testImplementation(libs.junit4)
}
