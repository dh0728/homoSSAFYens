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
        minSdk = 30
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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

    // Compose 사용 시
    buildFeatures { compose = true }
    // Kotlin 2.0 + compose plugin이면 보통 compilerExtensionVersion 별도 지정 불필요
}

dependencies {
    implementation(platform(libs.androidx.compose.bom.new))
    implementation(libs.androidx.ui)
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose")

    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.appcompat:appcompat:1.7.0-alpha03")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation(libs.play.services.location)
    implementation("com.google.android.gms:play-services-wearable:18.2.0")
    implementation("androidx.fragment:fragment-ktx:1.6.2")
    implementation(libs.androidx.fragment)

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation(libs.androidx.espresso.core.new)

    // Retrofit
    implementation("com.squareup.retrofit2:retrofit:2.11.0")

    // Gson 변환기 (JSON <-> Data Class 자동 변환)
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")

    // OkHttp (네트워크 통신)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

}