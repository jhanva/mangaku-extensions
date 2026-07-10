plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.mangaku.extension.senmanga"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.mangaku.extension.ja.senmanga"
        minSdk = 26
        targetSdk = 35
        // Mantener en sincronia con extension.json (el indice del repo se genera desde alli).
        versionCode = 2
        versionName = "1.0.2"
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
    compileOnly(project(":core:tachiyomi-api"))
    // Gson lo provee la app anfitriona en runtime (lo usa el parseo de la API JSON de SenManga).
    compileOnly(libs.gson)

    // Los tests unitarios si necesitan las clases en el classpath de ejecucion.
    testImplementation(project(":core:tachiyomi-api"))
    testImplementation(libs.gson)
    testImplementation(libs.junit4)
}
