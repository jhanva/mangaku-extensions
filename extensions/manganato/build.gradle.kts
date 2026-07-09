plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.mangaku.extension.manganato"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.mangaku.extension.en.manganato"
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
    // La plantilla MangaBox se BUNDLEA en el APK; el contrato de API y Gson los provee la app
    // anfitriona en runtime.
    implementation(project(":lib-multisrc:mangabox"))
    compileOnly(project(":core:tachiyomi-api"))

    testImplementation(project(":core:tachiyomi-api"))
    testImplementation(project(":lib-multisrc:mangabox"))
    testImplementation(libs.gson)
    testImplementation(libs.junit4)
}
