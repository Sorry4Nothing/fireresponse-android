plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// Die Konfigurationsdatei google-services.json liegt nur lokal (sie steht in der .gitignore). Ohne sie bleibt das Plug-in aus
// und der Build läuft wie bisher; Firebase ist dann nicht konfiguriert.
if (file("google-services.json").exists()) apply(plugin = "com.google.gms.google-services")

android {
    namespace = "io.github.sorry4nothing.cevios"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "io.github.sorry4nothing.cevios"
        minSdk = 26
        targetSdk = 37
        // versionCode = Major·10000 + Minor·100 + Patch; versionName im Format x.y.z, das das Backend verlangt (sonst 426).
        versionCode = 900
        versionName = "0.9.0"

        // Echte Adresse nur lokal (~/.gradle/gradle.properties oder -PceviApiBaseUrl), nie im Repo.
        val apiBaseUrl = providers.gradleProperty("ceviApiBaseUrl").getOrElse("https://api.example.org")
        buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    signingConfigs {
        // Schlüssel und Passwörter nur lokal (~/.gradle/gradle.properties), nie im Repo. Ohne sie bleibt die Release-Variante
        // unsigniert, damit CI und andere Rechner weiter bauen.
        providers.gradleProperty("ceviSigningStoreFile").orNull?.let { storePath ->
            create("release") {
                storeFile = file(storePath)
                storePassword = providers.gradleProperty("ceviSigningStorePassword").get()
                keyAlias = providers.gradleProperty("ceviSigningKeyAlias").get()
                keyPassword = providers.gradleProperty("ceviSigningKeyPassword").get()
            }
        }
    }

    buildTypes {
        release {
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
            signingConfigs.findByName("release")?.let { signingConfig = it }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.biometric)
    // Die Abfrage braucht FragmentActivity; biometric bringt nur eine alte Fassung mit.
    implementation(libs.androidx.fragment)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)
    testImplementation(libs.junit)
    testImplementation(libs.okhttp.mockwebserver)
}
