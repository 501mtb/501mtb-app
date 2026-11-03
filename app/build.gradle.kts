plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    // Enable this after adding app/google-services.json:
    // id("com.google.gms.google-services")
}

android {
    namespace = "mg.madagascar.mtb501"
    compileSdk = 37

    defaultConfig {
        applicationId = "mg.madagascar.mtb501"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2026.09.00"))
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3:1.4.0")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.9.6")

    // Firebase libraries are declared for the upcoming authentication/data integration.
    implementation(platform("com.google.firebase:firebase-bom:35.1.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
