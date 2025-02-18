plugins {
    id("com.android.application")
    id("kotlin-android")
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.0" // Використовуйте Compose Compiler версії 2.0.0
}

android {
    namespace = "com.serhio.homeaccountingapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.serhio.homeaccountingapp"
        minSdk = 24
        targetSdk = 35
        versionCode = 5
        versionName = "5.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            keyAlias = "my-alias"  // Введіть ваш псевдонім ключа
            keyPassword = "_25Boxer92_"  // Введіть пароль ключа
            storeFile = file("C:/Users/sereb/AndroidStudioProjects/HomeAccountingApp/my-release-key.jks")  // Введіть шлях до keystore файлу
            storePassword = "_25Boxer92_"  // Введіть пароль keystore
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
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
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "2.0.0" // Використовуйте відповідну версію Compose Compiler
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.10.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.0")
    implementation("androidx.activity:activity-compose:1.7.0")

    // Jetpack Compose BOM
    implementation(platform("androidx.compose:compose-bom:2024.01.00"))

    // Jetpack Compose Libraries
    implementation("androidx.compose.ui:ui:1.5.1")
    implementation("androidx.compose.foundation:foundation:1.5.1")
    implementation("androidx.compose.material3:material3:1.2.0")
    implementation("androidx.compose.material:material-icons-extended:1.5.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.1")
    implementation("androidx.compose.ui:ui-tooling-preview")

    // LiveData integration with Compose
    implementation("androidx.compose.runtime:runtime-livedata:1.5.1")
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // Navigation Component
    implementation("androidx.navigation:navigation-compose:2.6.0")

    // Material Design
    implementation("com.google.android.material:material:1.9.0")

    // MPAndroidChart
    implementation("com.github.PhilJay:MPAndroidChart:3.1.0")

    // Gson
    implementation("com.google.code.gson:gson:2.10")

    // Accompanist Pager
    implementation("com.google.accompanist:accompanist-pager:0.23.1")
    implementation("com.google.accompanist:accompanist-pager-indicators:0.23.1")

    // Debug Tools
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.5.1")

    // Material Dialogs Library
    implementation("io.github.vanpra.compose-material-dialogs:datetime:0.6.0")
}