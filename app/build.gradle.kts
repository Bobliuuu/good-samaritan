plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.android.libraries.mapsplatform.secrets.gradle.plugin)
    // Add the Google services Gradle plugin
    id("com.google.gms.google-services")
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.0" // this version matches your Kotlin version
}

val adminSecrets: Map<String, String> = rootProject.file("secrets.properties")
    .takeIf { it.exists() }
    ?.reader()
    ?.useLines { lines ->
        lines
            .filter { it.contains("=") }
            .map { it.substringBefore("=").trim() to it.substringAfter("=").trim() }
            .toMap()
    } ?: emptyMap()

val adminEmail = adminSecrets["ADMIN_EMAIL"] ?: "missing@example.com"
val adminPassword = adminSecrets["ADMIN_PASSWORD"] ?: "missing_password"
val openAiKey = adminSecrets["OPENAI_KEY"] ?: "missing_key"
val webClientKey = adminSecrets["GOOGLE_WEB_CLIENT_ID"] ?: "missing"


android {
    namespace = "com.example.myapplication"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.myapplication"
        minSdk = 26 // To run Caller
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        resourceConfigurations.add("en")

        resValue("string", "admin_email", "\"$adminEmail\"")
        resValue("string", "admin_password", "\"$adminPassword\"")
        buildConfigField("String", "OPENAI_KEY", "\"$openAiKey\"")
        resValue("string", "default_web_client_id", "\"$webClientKey\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        viewBinding = true
        compose = true
        buildConfig = true
    }

    sourceSets {
        getByName("test") {
            dependencies {
                testImplementation("androidx.compose.ui:ui-test-junit4:1.6.0")
                implementation(libs.androidx.ui.test.manifest)
                implementation(libs.robolectric)
            }
        }
    }

    testOptions.unitTests.isIncludeAndroidResources = true
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.play.services.maps)
    implementation("com.google.android.gms:play-services-location:21.2.0")
    implementation("com.google.android.libraries.places:places:3.4.0")
    implementation(libs.firebase.firestore.ktx)
    implementation(libs.places)
    implementation("io.coil-kt:coil-compose:2.4.0")
    implementation(libs.firebase.storage.ktx)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.ui.viewbinding)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // Import the Firebase BoM
    implementation(platform("com.google.firebase:firebase-bom:33.9.0"))
    // TODO: Add the dependencies for Firebase products you want to use
    // When using the BoM, don't specify versions in Firebase dependencies
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-storage")
    // Add the dependencies for any other desired Firebase products
    // https://firebase.google.com/docs/android/setup#available-libraries

    val composeBom = platform("androidx.compose:compose-bom:2025.01.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)
    // Material Design 3
    implementation("androidx.compose.material3:material3")
    // Android Studio Preview support
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // ViewPager2
    implementation("androidx.viewpager2:viewpager2:1.0.0")

    // Glide for image loading
    implementation("com.github.bumptech.glide:glide:4.11.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.11.0")

    implementation("com.google.android.material:material:1.3.0")

    // UI Tests
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // Optional - Included automatically by material, only add when you need
    // the icons but not the material library (e.g. when using Material3 or a
    // Optional - Integration with activities (required for setContext)
    implementation("androidx.activity:activity-compose:1.10.0")

    testImplementation("androidx.compose.ui:ui-test-junit4:1.6.0")

    testImplementation("io.mockk:mockk:1.13.8")

    testImplementation("org.robolectric:robolectric:4.14.1")
    testImplementation("androidx.navigation:navigation-testing:2.5.3")

    implementation(libs.androidx.material.icons.extended)

    implementation(libs.androidx.navigation.compose.vlatestversion)

    implementation("com.google.android.gms:play-services-maps:18.1.0")

    // Koin/Hilt
    implementation("io.insert-koin:koin-android:3.5.3")
    implementation("io.insert-koin:koin-androidx-compose:3.5.3")

    implementation("androidx.compose.runtime:runtime:1.5.0")

    testImplementation("org.mockito:mockito-core:4.8.0")
    testImplementation("org.mockito:mockito-inline:4.8.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:4.0.0")
    testImplementation("io.insert-koin:koin-android:3.5.3")
    testImplementation("io.insert-koin:koin-androidx-compose:3.5.3")
    testImplementation("org.apache.httpcomponents:httpclient:4.5.13")
    testImplementation("androidx.arch.core:core-testing:2.1.0")

    implementation("com.google.android.gms:play-services-auth:21.0.0")

    implementation("io.ktor:ktor-client-okhttp:2.3.4")
    implementation("io.ktor:ktor-client-core:2.3.4")
    implementation("io.ktor:ktor-client-serialization:2.3.4")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.4")


}

secrets {
    // To add your Maps API key to this project:
    // 1. If the secrets.properties file does not exist, create it in the same folder as the local.properties file.
    // 2. Add this line, where YOUR_API_KEY is your API key:
    //        MAPS_API_KEY=YOUR_API_KEY
    propertiesFileName = "secrets.properties"

    // A properties file containing default secret values. This file can be
    // checked in version control.
    defaultPropertiesFileName = "local.defaults.properties"
}

