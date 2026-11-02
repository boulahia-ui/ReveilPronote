plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.chaquo.python")
}

android {
    namespace = "com.reveilpronote.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.reveilpronote.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        ndk { abiFilters += listOf("arm64-v8a", "x86_64") }
    }
    buildTypes {
        release { isMinifyEnabled = false }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
    composeOptions { kotlinCompilerExtensionVersion = "1.5.14" }
    packaging { resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" } }
}

// Python embarqué (Chaquopy) pour utiliser la bibliothèque pronotepy
chaquopy {
    defaultConfig {
        version = "3.11"
        pip {
            // Aucune dépendance automatique : pycryptodome (code natif) est remplacé
            // par le module Crypto fourni. Toutes les dépendances sont listées ci-dessous.
            options("--no-deps")
            install("requests")
            install("urllib3")
            install("idna")
            install("certifi")
            install("charset-normalizer")
            install("beautifulsoup4")
            install("soupsieve")
            install("typing-extensions")
            install("autoslot")
            install("pronotepy==2.15.7")
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.09.00")
    implementation(composeBom)
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.5")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.work:work-runtime-ktx:2.9.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
    implementation("com.google.zxing:core:3.4.1")
}
