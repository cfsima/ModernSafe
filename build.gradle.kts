buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:9.1.0-alpha08")
        classpath("com.github.triplet.gradle:play-publisher:3.9.1")
    }
}

val ci: Boolean = "true" == System.getenv("CI")
val preDexEnabled: Boolean = "true" == System.getProperty("pre-dex", "true")

// Manifest version information!
val versionMajor = 2
val versionMinor = 0
val versionPatch = 0
val versionBuild = 1 // bump for dogfood builds, public betas, etc.

val versionCode = versionMajor * 10000 + versionMinor * 1000 + versionPatch * 100 + versionBuild
val versionName = "$versionMajor.$versionMinor.$versionPatch"

val compileSdkVersion = 35
val buildToolsVersion = "34.0.0"
val targetSdkVersion = 35

// Add extra properties to root project
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
