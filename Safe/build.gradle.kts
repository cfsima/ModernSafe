import com.android.build.api.dsl.ApplicationExtension
import java.io.FileInputStream
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
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
            isShrinkResources = true
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

    implementation("androidx.annotation:annotation:1.11.0")
    implementation("androidx.fragment:fragment-ktx:1.9.1")
    implementation("androidx.preference:preference-ktx:1.2.1")
    implementation("com.google.android.material:material:1.14.0")
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
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test:runner:1.7.0")
    androidTestImplementation("androidx.test:rules:1.7.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    androidTestImplementation("androidx.test.espresso:espresso-intents:3.7.0")
    androidTestImplementation(platform("androidx.compose:compose-bom:2026.09.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}
