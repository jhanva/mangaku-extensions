plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "eu.kanade.tachiyomi.multisrc.madara"
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

// Plantilla multisrc Madara. Se BUNDLEA en cada extension que la use (via implementation en su
// build); el contrato de API lo provee la app anfitriona en runtime, por eso es compileOnly.
dependencies {
    compileOnly(project(":core:tachiyomi-api"))
}
