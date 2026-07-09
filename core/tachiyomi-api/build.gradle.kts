plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "eu.kanade.tachiyomi"
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

// Contrato de la API de extensiones (eu.kanade.tachiyomi.* + shim Injekt). Copia sincronizada del
// mismo contrato que la app anfitriona implementa y provee en runtime; por eso las extensiones y las
// plantillas dependen de este modulo con compileOnly (no se bundlea en los APKs).
dependencies {
    implementation(libs.kotlinx.coroutines.android)
    api(libs.okhttp)
    api(libs.jsoup)
    testImplementation(libs.junit4)
    testImplementation(libs.kotlinx.coroutines.test)
}
