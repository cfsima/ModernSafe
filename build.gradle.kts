buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:9.5.1")
        classpath("org.jetbrains.kotlin:compose-compiler-gradle-plugin:2.2.10")
        classpath("org.jetbrains.kotlin:kotlin-serialization:2.2.10")
    }
}

val ci: Boolean = "true" == System.getenv("CI")

// Manifest version information!
val versionMajor = 2
val versionMinor = 0
val versionPatch = 0
val versionBuild = 1

val versionCode = versionMajor * 10000 + versionMinor * 1000 + versionPatch * 100 + versionBuild
val versionName = "$versionMajor.$versionMinor.$versionPatch"

val compileSdkVersion = 37
val buildToolsVersion = "37.0.0"
val targetSdkVersion = 35

extra["ci"] = ci
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