import java.io.FileInputStream
import java.util.Properties

plugins {
    id("com.android.application")
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(FileInputStream(localPropertiesFile))
}

android {
    namespace = "org.openintents.safe"
    compileSdk = (rootProject.extra["compileSdkVersion"] as Int)

    buildFeatures {
        buildConfig = true
    }

    signingConfigs {
        create("release") {
            storeFile = file("../settings/" + (localProperties["release_keystore_name"]?.toString() ?: "debug.keystore"))
            keyAlias = localProperties["release_keystore_alias"]?.toString() ?: "androiddebugkey"
            storePassword = localProperties["release_keystore_pwd"]?.toString() ?: "android"
            keyPassword = localProperties["release_keystore_pwd2"]?.toString() ?: "android"
        }
    }

    defaultConfig {
        applicationId = "org.openintents.safe"
        versionCode = (rootProject.extra["versionCode"] as Int)
        versionName = (rootProject.extra["versionName"] as String)
        minSdk = 23
        targetSdk = (rootProject.extra["targetSdkVersion"] as Int)
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    sourceSets {
        getByName("androidTest").setRoot("tests")
        getByName("debug").setRoot("build-types/debug")
        getByName("release").setRoot("build-types/release")
    }

    lint {
        abortOnError = false
    }

    buildTypes {
        getByName("debug") {
            signingConfig = signingConfigs.getByName("debug")
        }
        getByName("release") {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.txt")
            signingConfig = signingConfigs.getByName("release")
        }
        create("alpha") {
            initWith(getByName("release"))
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))

    implementation("androidx.annotation:annotation:1.7.0")
    implementation("androidx.fragment:fragment-ktx:1.6.2")
    implementation("androidx.preference:preference-ktx:1.2.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.appcompat:appcompat:1.6.1")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.annotation:annotation:1.7.0")
    androidTestImplementation("androidx.test:runner:1.5.2")
    androidTestImplementation("androidx.test:rules:1.5.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.test.espresso:espresso-intents:3.5.1")
}
