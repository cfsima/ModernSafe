buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        // Change from 9.0.0 to 8.13.0 to support Gradle 9.0.0
        classpath("com.android.tools.build:gradle:8.13.2")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.24")
        classpath("com.github.triplet.gradle:play-publisher:3.9.1")
    }
}

val ci: Boolean = "true" == System.getenv("CI")
val preDexEnabled: Boolean = "true" == System.getProperty("pre-dex", "true")

// Manifest version information!
val versionMajor = 2
val versionMinor = 0
val versionPatch = 0
val versionBuild = 1

val versionCode = versionMajor * 10000 + versionMinor * 1000 + versionPatch * 100 + versionBuild
val versionName = "$versionMajor.$versionMinor.$versionPatch"

// These versions are perfectly fine for AGP 8.13.0
val compileSdkVersion = 35
val buildToolsVersion = "35.0.0"
val targetSdkVersion = 35

extra["ci"] = ci
extra["preDexEnabled"] = preDexEnabled
extra["versionCode"] = versionCode
extra["versionName"] = versionName
extra["compileSdkVersion"] = compileSdkVersion
extra["buildToolsVersion"] = buildToolsVersion
extra["targetSdkVersion"] = targetSdkVersion

allprojects {
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("https://jitpack.io")
        }
    }
}