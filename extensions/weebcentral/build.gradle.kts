plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.mangaku.extension.weebcentral"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.mangaku.extension.en.weebcentral"
        minSdk = 26
        targetSdk = 35
        // Mantener en sincronia con extension.json (el indice del repo se genera desde alli).
        versionCode = 1
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
    // compileOnly: la app anfitriona provee eu.kanade.tachiyomi.*, OkHttp y Jsoup en runtime.
    // Asi el APK de la extension pesa unos pocos KB y no duplica clases.
    compileOnly(project(":core:tachiyomi-api"))

    // Los tests unitarios si necesitan las clases en el classpath de ejecucion.
    testImplementation(project(":core:tachiyomi-api"))
    testImplementation(libs.junit4)
}
