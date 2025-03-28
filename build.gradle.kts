// build.gradle.kts de nivel raíz (Project-level)


plugins {
    id("com.android.application") version "8.9.0" apply false
    id("com.android.library") version "8.9.0" apply false
    id("com.google.gms.google-services") version "4.4.2" apply false // Necesario para Firebase
    id("org.jetbrains.kotlin.plugin.serialization") version "2.1.0"
}

buildscript {
    repositories {
        google()
        mavenCentral() // Necesario para UCrop y otras dependencias
        maven("https://jitpack.io")  // Asegura que este repo esté presente
        maven("https://oss.sonatype.org/content/repositories/snapshots/")
        maven { url = uri("https://jitpack.io") }
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.9.1")
        classpath("com.google.gms:google-services:4.4.2")

    }
}

allprojects {
    repositories {
        google()
        mavenCentral() // Asegúrate de que esté presente
        maven { url = uri("https://jitpack.io") } // Agregar JitPack aquí
    }
}
