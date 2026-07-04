buildscript {
    repositories {
        mavenCentral()
        google()
    }
    dependencies {
        classpath("com.mobidevelop.robovm:robovm-gradle-plugin:2.3.25")
    }
}

plugins {
    kotlin("jvm") version "1.9.24" apply false
    kotlin("android") version "1.9.24" apply false
    id("com.android.application") version "8.6.1" apply false
    id("org.jlleitschuh.gradle.ktlint") version "12.1.1" apply false
}
