import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    // Supabase DTOs use kotlinx-serialization (@Serializable)
    alias(libs.plugins.kotlin.serialization)
}

// Read secrets from local.properties (never committed to git).
// SUPABASE_URL=... / SUPABASE_ANON_KEY=... / GEMINI_API_KEY=...
val localProps = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

android {
    namespace = "com.kerjalah.app"
    compileSdk {
        version = release(37) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.kerjalah.app"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Exposed to code as BuildConfig.SUPABASE_URL etc.
        buildConfigField("String", "SUPABASE_URL", "\"${localProps.getProperty("SUPABASE_URL", "")}\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"${localProps.getProperty("SUPABASE_ANON_KEY", "")}\"")
        buildConfigField("String", "GEMINI_API_KEY", "\"${localProps.getProperty("GEMINI_API_KEY", "")}\"")
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true // needed for buildConfigField above
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    // Material icons (Icons.Filled.* / Icons.AutoMirrored.*).
    // Newer material3 no longer pulls this in; the artifact is frozen at 1.7.8.
    implementation("androidx.compose.material:material-icons-core:1.7.8")
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
    //undirectional data workflow
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7") // provide viewModel()
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")  // provide collectAsStateWithLifecycle()
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0") // provide viewModelScope / Flow
    implementation("androidx.navigation:navigation-compose:2.7.7")
    // Supabase: Auth + PostgREST (database) + Realtime
    implementation(platform(libs.supabase.bom))
    implementation(libs.supabase.auth)
    implementation(libs.supabase.postgrest)
    implementation(libs.supabase.realtime)
    implementation(libs.ktor.client.cio)
}
