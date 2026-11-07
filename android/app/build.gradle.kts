plugins {
    id("com.android.application")
    // The Flutter Gradle Plugin must be applied after the Android and Kotlin Gradle plugins.
    id("dev.flutter.flutter-gradle-plugin")
}

android {
    namespace = "com.aadil.onward"
    compileSdk = flutter.compileSdkVersion
    ndkVersion = flutter.ndkVersion

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    defaultConfig {
        applicationId = "com.aadil.onward"
        // You can update the following values to match your application needs.
        // For more information, see: https://flutter.dev/to/review-gradle-config.
        minSdk = flutter.minSdkVersion
        targetSdk = flutter.targetSdkVersion
        // Uses the version code from pubspec.yaml. When using split APKs, 1000 * ABI_VERSION
        // is added automatically by Flutter. (https://developer.android.com/studio/build/configure-apk-splits#configure-APK-versions)
        // You can force using the value of versionCode by specifying the `-P force-version-code-ignoring-abi=true`
        // flag during build.
        versionCode = flutter.versionCode
        versionName = flutter.versionName
    }

    signingConfigs {
        create("release") {
            // CI passes a private key through these variables when the
            // ANDROID_KEYSTORE_BASE64 secret is set (see README).
            val privateKeystore = System.getenv("ONWARD_KEYSTORE_FILE")
            if (privateKeystore != null) {
                storeFile = file(privateKeystore)
                storePassword = System.getenv("ONWARD_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("ONWARD_KEY_ALIAS")
                keyPassword = System.getenv("ONWARD_KEY_PASSWORD")
            } else {
                // Fallback: a fixed key kept in the repo so every build carries
                // the same signature and installs as an update. It is public,
                // so it proves nothing about who built the APK.
                storeFile = file("onward-public.jks")
                storePassword = "onward-public"
                keyAlias = "onward"
                keyPassword = "onward-public"
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
    }
}

flutter {
    source = "../.."
}
