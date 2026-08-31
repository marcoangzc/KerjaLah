package com.kerjalah.advisor

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO as ClientCIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.cio.CIO as ServerCIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.server.routing.routing
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

// KerjaLah AI Advisor - a Kotlin/JVM Ktor server.
//
// Why it exists: the Groq key used to ship inside the APK and the AI verdict
// used to travel in the client's INSERT payload. Both now live here, behind a
// JWT check. See advisor/README.md for deployment.
fun main() {
    embeddedServer(ServerCIO, port = Config.port, host = "0.0.0.0") {
        module()
    }.start(wait = true)
}

fun Application.module() {
    val log = LoggerFactory.getLogger("Advisor")

    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true })
    }
    install(CallLogging)

    // A leaked stack trace can describe the schema to an attacker; log the
    // detail, return a flat message.
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            log.error("Unhandled failure", cause)
            call.respond(HttpStatusCode.InternalServerError, ErrorResponse("Internal error"))
        }
    }

    val http = HttpClient(ClientCIO) {
        install(ClientContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
        // The advisor call must not pin a request open: a slow model would
        // otherwise hold the student's apply tap hostage.
        install(HttpTimeout) {
            requestTimeoutMillis = 12_000
            connectTimeoutMillis = 5_000
        }
    }

    val supabase = SupabaseAdmin(http)
    val advisor = GroqAdvisor(http)

    if (Config.groqApiKey == null) {
        log.warn("GROQ_API_KEY is not set - applications will be saved without AI advice")
    }

    routing {
        advisorRoutes(supabase, advisor)
    }
}
