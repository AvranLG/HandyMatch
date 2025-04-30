plugins {
    id("com.android.application")
    id("com.google.gms.google-services") // 🔹 Agregado para Firebase



}

android {
    namespace = "com.example.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        buildConfigField("String", "MAPS_API_KEY", "\"${property("mapsApiKey")}\"")
        resValue("string", "maps_api_key", "mapsApiKey")







        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        manifestPlaceholders["allowBackup"] = "true"

    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            buildConfigField("String", "MAPS_API_KEY", "\"${property("mapsApiKey")}\"")
            debug {
                buildConfigField("String", "MAPS_API_KEY", "\"${property("mapsApiKey")}\"")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        viewBinding = true

        buildConfig = true
    }

}

dependencies {

    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.1")
    implementation("androidx.activity:activity:1.10.1")

    // Firebase - Dependencias esenciales
    implementation(platform("com.google.firebase:firebase-bom:33.11.0"))
    implementation("com.google.firebase:firebase-firestore:25.1.3")
    implementation("com.google.firebase:firebase-database")
    implementation("com.google.firebase:firebase-storage")
    implementation ("com.google.firebase:firebase-storage:21.0.1")

    implementation("com.google.firebase:firebase-database-ktx:21.0.0")
    implementation("com.google.firebase:firebase-auth-ktx:23.2.0")

    // Agregar la dependencia de UCrop para recortar imágenes
    implementation("com.github.yalantis:uCrop:2.2.9")

    // Dependencia para consultas HTTP necesario para las API
    implementation("com.android.volley:volley:1.2.1")

    implementation("androidx.core:core:1.15.0")
    implementation("androidx.core:core-ktx:1.15.0")

    implementation("androidx.navigation:navigation-fragment:2.8.9")
    implementation("androidx.navigation:navigation-ui:2.8.9")

    implementation("com.google.firebase:firebase-auth:23.2.0")  // Dependencia de Firebase Authentication
    implementation("com.google.android.gms:play-services-auth:21.3.0")  // Dependencia de Google Sign-In
    implementation ("com.facebook.android:facebook-android-sdk:18.0.2")

    implementation ("com.google.firebase:firebase-messaging:24.1.1")
    implementation("com.google.firebase:firebase-inappmessaging-display")
    implementation("com.google.firebase:firebase-analytics")


    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.cloudinary:cloudinary-android:2.3.1")

    implementation("com.squareup.okhttp3:okhttp:4.10.0")

    implementation ("com.google.android.gms:play-services-auth:21.3.0")

    // Glide para cargar imágenes desde URLs
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")

    //Para maps
    implementation ("com.google.android.gms:play-services-maps:19.1.0")  // Para Maps
    implementation ("com.google.android.gms:play-services-location:21.3.0")  // Para obtener la ubicación actual



    // Dependencias de red
    implementation("com.squareup.okhttp3:okhttp:4.10.0")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0")


    implementation("io.github.jan-tennert.supabase:postgrest-kt:0.7.6")

    // Opcional: Para manejo de JSON
    implementation("com.google.code.gson:gson:2.8.9")
    implementation ("com.google.android.libraries.places:places:3.3.0")

    //Dependencia necesaria para mapa
    // OSMDroid - biblioteca principal
    implementation ("org.osmdroid:osmdroid-android:6.1.16")

    // Para soporte de localización (opcional)
    //implementation ("org.osmdroid:osmdroid-geopackage:6.1.16")


    // Dependencias de prueba
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    implementation("com.google.android.gms:play-services-auth:20.7.0")

}

