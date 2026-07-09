plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.mangaku.extension.inmanga"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.mangaku.extension.es.inmanga"
        minSdk = 26
        targetSdk = 35
        // Mantener en sincronia con extension.json (el indice del repo se genera desde alli).
        versionCode = 1
        versionName = "1.0.0"
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
    // El contrato de API, OkHttp, Jsoup y Gson los provee la app anfitriona en runtime.
    compileOnly(project(":core:tachiyomi-api"))
    compileOnly(libs.gson)

    testImplementation(project(":core:tachiyomi-api"))
    testImplementation(libs.gson)
    testImplementation(libs.junit4)
}
