plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "eu.kanade.tachiyomi.multisrc.mangabox"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

// Plantilla multisrc MangaBox (Manganato). Se BUNDLEA en cada extension que la use; el
// contrato de API y Gson los provee la app anfitriona en runtime, por eso son compileOnly.
dependencies {
    compileOnly(project(":core:tachiyomi-api"))
    compileOnly(libs.gson)
    testImplementation(project(":core:tachiyomi-api"))
    testImplementation(libs.gson)
    testImplementation(libs.junit4)
}
