plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    application
}

// Plain Kotlin/JVM - no Android here. This module is deployed to a server,
// never packaged into the APK, so it can hold the Groq key safely.
kotlin {
    jvmToolchain(17)
}

application {
    mainClass = "com.kerjalah.advisor.MainKt"
}

dependencies {
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.cio)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.server.status.pages)
    implementation(libs.ktor.server.call.logging)
    implementation(libs.ktor.serialization.json)
    // Outbound calls: Groq, and Supabase's REST/auth endpoints.
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.logback.classic)

    testImplementation(libs.junit)
    testImplementation(libs.ktor.server.test.host)
}
