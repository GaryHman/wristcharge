plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.meneer.wristcharge"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.meneer.wristcharge"   // must match the phone app for the Data Layer
        minSdk = 30                                // Wear OS 3+
        targetSdk = 35
        versionCode = 100001                       // distinct from the phone app (Play needs unique codes)
        versionName = "0.1.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }

dependencies {
    implementation("com.google.android.gms:play-services-wearable:19.0.0")
}
