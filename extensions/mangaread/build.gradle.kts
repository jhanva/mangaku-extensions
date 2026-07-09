plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.mangaku.extension.mangaread"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.mangaku.extension.mangaread"
        minSdk = 26
        targetSdk = 35
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
    // La plantilla Madara se BUNDLEA en el APK; el contrato de API lo provee la app anfitriona.
    implementation(project(":lib-multisrc:madara"))
    compileOnly(project(":core:tachiyomi-api"))

    testImplementation(project(":core:tachiyomi-api"))
    testImplementation(project(":lib-multisrc:madara"))
    testImplementation(libs.junit4)
}
