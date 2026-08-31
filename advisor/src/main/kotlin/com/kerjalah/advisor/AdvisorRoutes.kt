package com.kerjalah.advisor

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("AdvisorRoutes")

fun Route.advisorRoutes(supabase: SupabaseAdmin, advisor: GroqAdvisor) {

    // Hosting platforms poll this to decide if the instance is alive.
    get("/health") {
        call.respondText("ok")
    }

    /**
     * POST /assess-application   body: {"jobId": "<uuid>"}
     *
     * The phone sends one thing - "I want to apply to job X" - and the server
     * decides everything else. That is the whole point of this module: the
     * Groq key never reaches a device, and a student cannot hand-write their
     * own ai_match_percent, because the client no longer writes the row.
     */
    post("/assess-application") {
        val token = call.request.headers["Authorization"]
            ?.removePrefix("Bearer ")
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
        if (token == null) {
            call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Missing Authorization header"))
            return@post
        }

        // Identity comes from the verified token, never from the request body,
        // so a caller can only ever apply as themselves.
        val userId = supabase.verifyCaller(token)
        if (userId == null) {
            call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Invalid or expired session"))
            return@post
        }

        val jobId = runCatching { call.receive<ApplyRequest>().jobId }.getOrNull()
            ?.takeIf { it.isNotBlank() }
        if (jobId == null) {
            call.respond(HttpStatusCode.BadRequest, ErrorResponse("jobId is required"))
            return@post
        }

        // Everything below runs with the service role, which ignores RLS -
        // hence the explicit role check that the database would have made.
        val profile = supabase.findProfile(userId)
        if (profile == null) {
            call.respond(HttpStatusCode.Forbidden, ErrorResponse("No profile for this account"))
            return@post
        }
        if (profile.role != "STUDENT") {
            call.respond(HttpStatusCode.Forbidden, ErrorResponse("Only students can apply"))
            return@post
        }

        val job = supabase.findJob(jobId)
        if (job == null) {
            call.respond(HttpStatusCode.NotFound, ErrorResponse("Job not found"))
            return@post
        }

        // Best effort: null here means the row is still written, just without
        // advice. The AI advises, it never gates the application.
        val assessment = advisor.assess(job, profile)
        if (assessment == null) log.warn("Inserting application without advice (job ${job.id})")

        val result = supabase.insertApplication(
            ApplicationInsert(
                jobId = job.id,
                studentId = userId,
                aiMatchPercent = assessment?.matchPercent,
                aiSuggestedStatus = assessment?.suggestedStatus?.name,
                aiReason = assessment?.reason,
            ),
        )

        when (result) {
            is InsertResult.Created -> call.respond(
                ApplyResponse(
                    applied = true,
                    applicationId = result.id,
                    aiAdviceAvailable = assessment != null,
                ),
            )

            InsertResult.Duplicate -> call.respond(
                HttpStatusCode.Conflict,
                ApplyResponse(applied = false, duplicate = true),
            )

            InsertResult.Failed -> call.respond(
                HttpStatusCode.InternalServerError,
                ErrorResponse("Could not save your application"),
            )
        }
    }
}
