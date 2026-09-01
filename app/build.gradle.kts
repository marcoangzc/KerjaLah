import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    // Supabase DTOs use kotlinx-serialization (@Serializable)
    alias(libs.plugins.kotlin.serialization)
}

// Read secrets from local.properties (never committed to git). See
// local.properties.example.
//
// Caveat worth knowing: buildConfigField compiles these values into the APK as
// plain string constants, so anyone who decompiles it can read them. That is
// fine for SUPABASE_ANON_KEY, which is designed to be public and is policed by
// Row Level Security - but GROQ_API_KEY is a real secret and is only here
// because the app calls Groq directly. See the note on AiClient.
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
        buildConfigField("String", "GROQ_API_KEY", "\"${localProps.getProperty("GROQ_API_KEY", "")}\"")
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
    testOptions {
        unitTests {
            // android.util.Log is a stub in unit tests and throws by default.
            // AiClient logs while parsing, so let those calls no-op instead.
            isReturnDefaultValues = true
        }
    }
}

kotlin {
    compilerOptions {
        // Application.appliedAt is a kotlin.time.Instant, still @ExperimentalTime
        // in Kotlin 2.2.x. The opt-in is module-wide because the requirement
        // spreads to every call site that merely compares two timestamps
        // (the sortedByDescending in each list ViewModel).
        optIn.add("kotlin.time.ExperimentalTime")
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

