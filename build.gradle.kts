// build.gradle.kts de nivel raíz (Project-level)


plugins {
    id("com.android.application") version "8.2.2" apply false
    id("com.android.library") version "8.2.2" apply false
    id("com.google.gms.google-services") version "4.4.2" apply false // Necesario para Firebase
}

buildscript {
    repositories {
        google()
        mavenCentral() // Necesario para UCrop y otras dependencias
        jcenter() // Opcional, pero puede ayudar a resolver algunas dependencias
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.2.2")
        classpath("com.google.gms:google-services:4.4.2")
    }
}

allprojects {
    repositories {
        google()
        mavenCentral() // Asegúrate de que esté presente
        jcenter() // Opcional
        maven { url = uri("https://jitpack.io") } // Agregar JitPack aquí
    }
}
