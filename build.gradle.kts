import com.android.build.api.dsl.ApplicationExtension
import java.io.File
import java.util.Properties

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
}

// Firma release compartida para los APKs de extension. Las credenciales se leen de keystore.properties
// (git-ignored) o de variables de entorno para CI. Si faltan, el release queda sin firmar (y la app en
// release lo rechazara por firma no confiable). El SHA-256 de este certificado esta registrado en la
// app anfitriona (ExtensionModule.TRUSTED_SIGNATURE_HASHES).
val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}
val storeFileProp: String? =
    keystoreProperties.getProperty("storeFile") ?: System.getenv("MANGAKU_STORE_FILE")
val storePasswordProp: String? =
    keystoreProperties.getProperty("storePassword") ?: System.getenv("MANGAKU_STORE_PASSWORD")
val keyAliasProp: String? =
    keystoreProperties.getProperty("keyAlias") ?: System.getenv("MANGAKU_KEY_ALIAS")
val keyPasswordProp: String? =
    keystoreProperties.getProperty("keyPassword") ?: System.getenv("MANGAKU_KEY_PASSWORD")
val hasReleaseSigning = storeFileProp != null && storePasswordProp != null &&
    keyAliasProp != null && keyPasswordProp != null

// Una ruta relativa se resuelve contra la raiz del proyecto; una absoluta (p. ej. el keystore
// decodificado en CI) se usa tal cual.
val resolvedStoreFile: File? = storeFileProp?.let {
    val f = File(it)
    if (f.isAbsolute) f else rootProject.file(it)
}

subprojects {
    if (path.startsWith(":extensions:")) {
        plugins.withId("com.android.application") {
            extensions.configure<ApplicationExtension> {
                if (hasReleaseSigning) {
                    signingConfigs.create("release") {
                        storeFile = resolvedStoreFile
                        storePassword = storePasswordProp
                        keyAlias = keyAliasProp
                        keyPassword = keyPasswordProp
                    }
                    buildTypes.getByName("release") {
                        signingConfig = signingConfigs.getByName("release")
                    }
                }
            }
        }
    }
}
