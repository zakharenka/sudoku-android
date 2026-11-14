plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "me.zakharenka.sudoku"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "me.zakharenka.sudoku"
        minSdk = 26
        targetSdk = 37

        val appVersionName = providers.gradleProperty("app.version").getOrElse("1.0.0")
        val versionParts = appVersionName.split(".")
        require(versionParts.size == 3) {
            "app.version must use semantic versioning: MAJOR.MINOR.PATCH"
        }

        versionCode = versionParts[0].toInt() * 1_000_000 +
            versionParts[1].toInt() * 1_000 +
            versionParts[2].toInt()
        versionName = appVersionName
    }

    buildTypes {
        release {
            optimization {
                enable = true
                // Do not add packageScope here: it caused an IllegalAccessError
                // in the release build (Kotlin collections / androidx.startup).
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)

    testImplementation(libs.junit)
}
