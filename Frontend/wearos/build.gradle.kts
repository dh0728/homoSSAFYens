plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.example.dive"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.dive"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Compose BOM
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))

    // Compose UI
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // OSMDroid (Interactive Map)
    implementation("org.osmdroid:osmdroid-android:6.1.18")

    // Compose Foundation (중요! - HorizontalPager 등을 위해)
    implementation("androidx.compose.foundation:foundation")

    // Wear Compose
    implementation("androidx.wear.compose:compose-material:1.2.0")
    implementation("androidx.wear.compose:compose-navigation:1.2.0")
    implementation("androidx.wear.compose:compose-foundation:1.2.0")

    // Activity & Lifecycle
    implementation("androidx.activity:activity-compose:1.8.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.2")

    // Navigation
    implementation("androidx.navigation:navigation-runtime-ktx:2.7.5")
    implementation("androidx.navigation:navigation-compose:2.7.5")

    // Wear specific
    implementation("androidx.wear:wear:1.3.0")
    implementation("androidx.wear:wear-ongoing:1.0.0")

    // Tiles (Tile 서비스용)
    implementation("androidx.wear.tiles:tiles:1.2.0")
    implementation("androidx.wear.tiles:tiles-material:1.2.0")
    implementation("androidx.concurrent:concurrent-futures:1.2.0")

    // Watchface Complications
    implementation("androidx.wear.watchface:watchface:1.2.1")
    implementation("androidx.wear.watchface:watchface-complications-data:1.2.1")
    implementation("androidx.wear.watchface:watchface-complications-data-source:1.2.1")
    implementation("androidx.wear.watchface:watchface-complications-data-source-ktx:1.2.1")

    // Google Play Services
    implementation("com.google.android.gms:play-services-wearable:18.1.0")
    implementation("com.google.android.gms:play-services-tasks:18.0.2")

    // Material Icons
    implementation("androidx.compose.material:material-icons-extended:1.5.4")

    // Core
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.core:core-splashscreen:1.0.1")

    // Coroutines (중요!)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")

    // Preferences
    implementation("androidx.preference:preference-ktx:1.2.1")

    // JSON & Utilities
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("com.google.guava:guava:32.1.3-android")

    // LocalBroadcastManager (중요!)
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("androidx.localbroadcastmanager:localbroadcastmanager:1.1.0")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}