// Root build file: ONLY declares plugin versions for the whole project.
// The real configuration lives in app/build.gradle.kts.
// "apply false" = load the plugin here, apply it inside modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.kotlin.jvm) apply false
}
