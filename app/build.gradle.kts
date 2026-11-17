plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.controllg.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.controllg.app"
        minSdk = 21
        targetSdk = 35
        // En GitHub Actions cada compilación sube el número (la app lo compara para avisar
        // de actualizaciones). Fuera de GitHub queda en 2.
        val run = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull()
        versionCode = if (run != null) 100 + run else 2
        versionName = if (run != null) "2.$run" else "2.0"
    }

    // Clave fija: así cada versión nueva se instala encima de la anterior sin desinstalar.
    // Es una clave de uso personal (el proyecto es público); no sirve para Google Play.
    signingConfigs {
        create("fija") {
            storeFile = file("control-lg.keystore")
            storePassword = "controllg"
            keyAlias = "controllg"
            keyPassword = "controllg"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("fija")
        }
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("fija")
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
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
}
