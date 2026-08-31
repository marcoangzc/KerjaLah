package com.kerjalah.advisor

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ---------- API surface (what the Android app sends and receives) ----------

@Serializable
data class ApplyRequest(val jobId: String)

@Serializable
data class ApplyResponse(
    val applied: Boolean,
    val duplicate: Boolean = false,
    val applicationId: String? = null,
    val aiAdviceAvailable: Boolean = false,
)

@Serializable
data class ErrorResponse(val error: String)

// ---------- Supabase row shapes (only the columns this server reads) ----------

@Serializable
data class AuthUser(val id: String)

@Serializable
data class ProfileRow(
    val id: String,
    val role: String,
    val organization: String = "",
    val bio: String = "",
)

@Serializable
data class JobRow(
    val id: String,
    val title: String,
    @SerialName("company_name") val companyName: String,
    val location: String,
    @SerialName("pay_per_hour") val payPerHour: Double,
    @SerialName("hours_per_week") val hoursPerWeek: Int,
    val description: String,
)

// applied_at is absent on purpose: the column defaults to now() and is
// immutable afterwards, so the timestamp is the database's, not this server's.
@Serializable
data class ApplicationInsert(
    @SerialName("job_id") val jobId: String,
    @SerialName("student_id") val studentId: String,
    val status: String = "PENDING",
    @SerialName("ai_match_percent") val aiMatchPercent: Int? = null,
    @SerialName("ai_suggested_status") val aiSuggestedStatus: String? = null,
    @SerialName("ai_reason") val aiReason: String? = null,
)

@Serializable
data class InsertedApplication(val id: String)

// ---------- Advisor result ----------

// Mirrors the DB CHECK constraint applications_ai_suggested_status_check.
// Deliberately not the same vocabulary as applications.status: the AI
// describes fit, it does not decide an outcome.
enum class AiSuggestedStatus { STRONG_MATCH, POSSIBLE_MATCH, WEAK_MATCH }

data class Assessment(
    val matchPercent: Int,
    val suggestedStatus: AiSuggestedStatus,
    val reason: String,
)
