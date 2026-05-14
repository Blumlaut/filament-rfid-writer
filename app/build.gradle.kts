plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.blumlaut.filamenttagwriter"
    compileSdk = 35
    buildToolsVersion = "37.0.0"

    defaultConfig {
        applicationId = "com.blumlaut.filamenttagwriter"
        minSdk = 26
        targetSdk = 35
        versionCode = (project.properties["versionCode"] as? String)?.toIntOrNull() ?: 1
        versionName = project.properties["versionName"] as? String ?: "1.0"
    }

    signingConfigs {
        create("release") {
            val keystorePath = project.layout.projectDirectory.file("keystore.jks")
            if (keystorePath.asFile.exists()) {
                storeFile = keystorePath.asFile
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            // Only sign if keystore and env vars are present (CI)
            val ks = signingConfigs.findByName("release")
            if (ks?.storeFile?.exists() == true && System.getenv("KEYSTORE_PASSWORD") != null) {
                signingConfig = ks
            }
        }
        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlin {
        jvmToolchain(21)
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.activity.compose)

    // Room (local database for filament catalog)
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)

    // ViewModel + LiveData for Compose
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
