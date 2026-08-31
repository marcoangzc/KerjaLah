package com.kerjalah.advisor

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import org.slf4j.LoggerFactory

// Thin wrapper over Supabase's REST endpoints.
//
// Two very different privilege levels live in this file, so they are kept in
// separate methods with the difference spelled out:
//   - verifyCaller() forwards the CALLER's token, proving who they are.
//   - everything else uses the service role key, which bypasses RLS entirely
//     and therefore relies on the authorisation checks in AdvisorRoutes.
class SupabaseAdmin(private val http: HttpClient) {

    private val log = LoggerFactory.getLogger(SupabaseAdmin::class.java)

    private val authUrl = "${Config.supabaseUrl}/auth/v1"
    private val restUrl = "${Config.supabaseUrl}/rest/v1"

    /**
     * Validates the caller's JWT with Supabase and returns their user id.
     * Null means the token is missing, malformed, expired or revoked.
     *
     * We deliberately ask the auth server rather than decoding the token
     * ourselves: signature and expiry checks are exactly the thing that is easy
     * to get subtly wrong by hand.
     */
    suspend fun verifyCaller(bearerToken: String): String? = runCatching {
        val response = http.get("$authUrl/user") {
            header("apikey", Config.supabaseAnonKey)
            header("Authorization", "Bearer $bearerToken")
        }
        if (!response.status.isSuccess()) return@runCatching null
        response.body<AuthUser>().id
    }.onFailure { log.error("Caller verification failed", it) }.getOrNull()

    suspend fun findProfile(userId: String): ProfileRow? = runCatching {
        http.get("$restUrl/profiles") {
            serviceRole()
            parameter("id", "eq.$userId")
            parameter("select", "id,role,organization,bio")
        }.body<List<ProfileRow>>().firstOrNull()
    }.onFailure { log.error("Profile lookup failed", it) }.getOrNull()

    suspend fun findJob(jobId: String): JobRow? = runCatching {
        http.get("$restUrl/jobs") {
            serviceRole()
            parameter("id", "eq.$jobId")
            parameter("select", "id,title,company_name,location,pay_per_hour,hours_per_week,description")
        }.body<List<JobRow>>().firstOrNull()
    }.onFailure { log.error("Job lookup failed", it) }.getOrNull()

    /**
     * Inserts the application. Returns the new row's id, or DUPLICATE if the
     * student already applied (Postgres 23505 on unique (job_id, student_id)).
     */
    suspend fun insertApplication(row: ApplicationInsert): InsertResult {
        val response: HttpResponse = runCatching {
            http.post("$restUrl/applications") {
                serviceRole()
                header("Prefer", "return=representation")
                contentType(ContentType.Application.Json)
                setBody(row)
            }
        }.getOrElse {
            log.error("Insert application failed", it)
            return InsertResult.Failed
        }

        if (response.status.isSuccess()) {
            val id = runCatching { response.body<List<InsertedApplication>>().firstOrNull()?.id }
                .getOrNull() ?: return InsertResult.Failed
            return InsertResult.Created(id)
        }

        val bodyText = runCatching { response.bodyAsText() }.getOrDefault("")
        if (response.status == HttpStatusCode.Conflict || bodyText.contains("23505")) {
            return InsertResult.Duplicate
        }
        log.error("Insert application rejected: ${response.status} $bodyText")
        return InsertResult.Failed
    }

    // service_role bypasses Row Level Security - callers must have already
    // established that this action is allowed.
    private fun io.ktor.client.request.HttpRequestBuilder.serviceRole() {
        header("apikey", Config.serviceRoleKey)
        header("Authorization", "Bearer ${Config.serviceRoleKey}")
    }
}

sealed interface InsertResult {
    data class Created(val id: String) : InsertResult
    data object Duplicate : InsertResult
    data object Failed : InsertResult
}
