package com.kerjalah.app.data.data

import android.util.Log
import com.kerjalah.app.BuildConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

// [B] AI advisor result. The AI only SUGGESTS - the employer decides.
data class AiAssessment(
    val matchPercent: Int,
    val suggestedStatus: String, // "ACCEPTED" or "REJECTED"
    val reason: String,
)

// [B] Module 3 (AI phase) - one small client for the Gemini API.
// Called ONCE per application, in the background, when a student applies.
// Any failure returns null: applying must NEVER be blocked by the AI.
object GeminiClient {

    private const val TAG = "GeminiClient"
    // gemini-2.5-flash returns 404 for new projects/keys (superseded by 3.x);
    // 3.5-flash is the current stable flash model.
    private const val MODEL = "gemini-3.5-flash"
    private const val URL =
        "https://generativelanguage.googleapis.com/v1beta/models/$MODEL:generateContent"

    // Reuse the Ktor engine supabase-kt already brought in.
    private val http = HttpClient(CIO)
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun assessApplication(job: Job, student: User): AiAssessment? = runCatching {
        val prompt = """
            You are a hiring assistant for part-time student jobs in Malaysia.
            Rate how well this student matches this job.
            Reply ONLY with JSON in exactly this shape:
            {"matchPercent": <integer 0-100>, "suggestedStatus": "ACCEPTED" or "REJECTED", "reason": "<one short sentence>"}

            Job: ${job.title} at ${job.companyName}, ${job.location}.
            Pay: RM ${job.payPerHour} per hour, ${job.hoursPerWeek} hours per week.
            Description: ${job.description}

            Student: ${student.name}, ${student.organization.ifBlank { "university not stated" }}.
            Bio: ${student.bio.ifBlank { "(no bio provided)" }}
        """.trimIndent()

        // Request body per the Gemini REST API; ask for pure JSON back.
        val body = buildJsonObject {
            putJsonArray("contents") {
                addJsonObject {
                    putJsonArray("parts") {
                        addJsonObject { put("text", prompt) }
                    }
                }
            }
            putJsonObject("generationConfig") {
                put("responseMimeType", "application/json")
            }
        }

        val response = http.post(URL) {
            contentType(ContentType.Application.Json)
            header("x-goog-api-key", BuildConfig.GEMINI_API_KEY)
            setBody(body.toString())
        }
        val raw = response.bodyAsText()
        if (!response.status.isSuccess()) error("Gemini HTTP ${response.status}: $raw")

        // Dig out candidates[0].content.parts[0].text, then parse that JSON.
        val text = json.parseToJsonElement(raw)
            .jsonObject.getValue("candidates").jsonArray[0]
            .jsonObject.getValue("content")
            .jsonObject.getValue("parts").jsonArray[0]
            .jsonObject.getValue("text").jsonPrimitive.content
        val obj = json.parseToJsonElement(text).jsonObject

        AiAssessment(
            matchPercent = obj.getValue("matchPercent").jsonPrimitive.int.coerceIn(0, 100),
            suggestedStatus = obj.getValue("suggestedStatus").jsonPrimitive.content,
            reason = obj.getValue("reason").jsonPrimitive.content,
        )
    }.onFailure { Log.e(TAG, "Gemini call failed", it) }.getOrNull()
}
