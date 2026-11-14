plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.roborazzi)
}

// CI passes -PversionName=2.0.57 -PversionCode=10057. Version codes start at
// 10000 so this build installs as an update over the earlier Flutter
// releases, whose per-ABI codes stayed below 5000.
val onwardVersion = providers.gradleProperty("onward.version").get()
val ciVersionName = providers.gradleProperty("versionName").orNull
val ciVersionCode = providers.gradleProperty("versionCode").orNull?.toInt()

android {
    namespace = "com.aadil.onward"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.aadil.onward"
        minSdk = 26
        targetSdk = 36
        versionCode = ciVersionCode ?: 10000
        versionName = ciVersionName ?: "$onwardVersion.0-dev"
    }

    // Release builds are signed with the private key CI decodes from the
    // ANDROID_KEYSTORE_* repository secrets (see README). Without it (local
    // builds, pull requests) they fall back to the debug key.
    val releaseKeystore = System.getenv("ONWARD_KEYSTORE_FILE")
    signingConfigs {
        if (releaseKeystore != null) {
            create("release") {
                storeFile = file(releaseKeystore)
                storePassword = System.getenv("ONWARD_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("ONWARD_KEY_ALIAS")
                keyPassword = System.getenv("ONWARD_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName(
                if (releaseKeystore != null) "release" else "debug",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.kotlinx.serialization.json)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.androidx.test.core)
    testImplementation(platform(libs.compose.bom))
    testImplementation(libs.compose.ui.test.junit4)
}
