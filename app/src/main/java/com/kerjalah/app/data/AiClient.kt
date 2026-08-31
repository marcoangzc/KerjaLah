package com.kerjalah.app.data

import android.util.Log
import com.kerjalah.app.BuildConfig
import io.github.jan.supabase.auth.auth
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// [B] Module 3 (AI phase) - the app's door to the AI advisor.
//
// This used to call Groq directly, with the API key compiled into the APK.
// Anything inside an APK can be extracted from it, so the whole advisor moved
// to the :advisor Ktor server, which holds the key and writes the application
// row itself. What is left here is the client half of that conversation.
//
// Note how little the app now sends: a job id. It does NOT send its own user
// id, the score, or the timestamp - the server derives all three, so a student
// cannot post themselves a 99% match.
object AiClient {

    private const val TAG = "AiClient"

    private val http = HttpClient(CIO)
    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    private data class ApplyRequest(val jobId: String)

    /** Outcome of asking the advisor to file an application. */
    enum class ApplyOutcome { CREATED, DUPLICATE, FAILED }

    /**
     * Asks the advisor server to score and file this application.
     *
     * The AI never gates anything: if Groq is down or slow, the server still
     * writes the row and simply leaves the ai_* columns null. FAILED here means
     * the application itself did not happen - a network or auth problem.
     */
    suspend fun assessAndApply(jobId: String): ApplyOutcome {
        // The server authenticates the caller with this token and takes the
        // student id from it, so an expired session must fail loudly rather
        // than quietly apply as nobody.
        val token = SupabaseClientProvider.client.auth.currentAccessTokenOrNull()
        if (token == null) {
            Log.e(TAG, "Apply aborted: no access token (session expired?)")
            return ApplyOutcome.FAILED
        }

        return runCatching {
            val response = http.post("${BuildConfig.ADVISOR_BASE_URL.trimEnd('/')}/assess-application") {
                contentType(ContentType.Application.Json)
                header("Authorization", "Bearer $token")
                setBody(json.encodeToString(ApplyRequest(jobId)))
            }

            when {
                response.status.isSuccess() -> {
                    Log.i(TAG, "Application filed for job $jobId")
                    ApplyOutcome.CREATED
                }
                // The server already knows about this application; the student
                // is not told an error happened, because nothing went wrong.
                response.status == HttpStatusCode.Conflict -> ApplyOutcome.DUPLICATE
                else -> error("advisor HTTP ${response.status}")
            }
        }.onFailure { Log.e(TAG, "Advisor call failed", it) }
            .getOrDefault(ApplyOutcome.FAILED)
    }
}
