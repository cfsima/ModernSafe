import com.android.build.api.dsl.ApplicationExtension
import java.io.FileInputStream
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(FileInputStream(localPropertiesFile))
}

configure<ApplicationExtension> {
    namespace = "io.github.cfsima.modernsafe"
    compileSdk = (rootProject.extra["compileSdkVersion"] as Int)

    buildFeatures {
        buildConfig = true
        compose = true
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
        applicationId = "io.github.cfsima.modernsafe"
        versionCode = (rootProject.extra["versionCode"] as Int)
        versionName = (rootProject.extra["versionName"] as String)
        minSdk = 33
        targetSdk = (rootProject.extra["targetSdkVersion"] as Int)
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    sourceSets {
        getByName("androidTest").setRoot("tests")
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
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation("androidx.annotation:annotation:1.11.0")
    implementation("androidx.datastore:datastore-preferences:1.3.0")
    implementation("androidx.appcompat:appcompat:1.8.0")

    // Compose Dependencies
    val composeBom = platform("androidx.compose:compose-bom:2026.09.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0")
    implementation("androidx.navigation:navigation-compose:2.8.8")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.11.0")
    androidTestImplementation("androidx.test:runner:1.7.0")
    androidTestImplementation("androidx.test:rules:1.7.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    androidTestImplementation("androidx.test.espresso:espresso-intents:3.7.0")
    androidTestImplementation(platform("androidx.compose:compose-bom:2026.09.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}
