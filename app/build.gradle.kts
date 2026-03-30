plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.zuno"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.zuno"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }
}

dependencies {

    implementation(platform("androidx.compose:compose-bom:2024.02.01"))

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation(platform("com.google.firebase:firebase-bom:32.8.1"))


        implementation(platform("com.google.firebase:firebase-bom:32.7.0"))

        implementation("com.google.firebase:firebase-storage-ktx")
        implementation("com.google.firebase:firebase-auth-ktx")
        implementation("com.google.firebase:firebase-firestore-ktx")


    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("androidx.activity:activity-compose:1.8.2")

    debugImplementation("androidx.compose.ui:ui-tooling")
}