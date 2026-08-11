package com.kerjalah.app.data.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// [A] DTOs: exact shapes of the Supabase table rows.
// Why separate from the domain models: table columns are snake_case and
// insert rows have no id (the database generates it). Mapping here keeps
// the domain models - and everything above them - unchanged (UDF layering).

// ---------- profiles ----------

@Serializable
data class ProfileRow(
    val id: String,
    val role: String,
    val name: String,
    val email: String,
    val organization: String = "",
    val bio: String = "",
)

fun ProfileRow.toDomain() = User(
    id = id,
    role = UserRole.valueOf(role),
    name = name,
    email = email,
    password = "", // Supabase Auth owns passwords now; never stored here
    organization = organization,
    bio = bio,
)

// ---------- jobs ----------

@Serializable
data class JobRow(
    val id: String,
    @SerialName("employer_id") val employerId: String,
    val title: String,
    @SerialName("company_name") val companyName: String,
    val location: String,
    @SerialName("pay_per_hour") val payPerHour: Double,
    @SerialName("hours_per_week") val hoursPerWeek: Int,
    val description: String,
)

fun JobRow.toDomain() = Job(
    id = id,
    employerId = employerId,
    title = title,
    companyName = companyName,
    location = location,
    payPerHour = payPerHour,
    hoursPerWeek = hoursPerWeek,
    description = description,
)

// Insert payload: no id - Postgres generates it.
@Serializable
data class JobInsert(
    @SerialName("employer_id") val employerId: String,
    val title: String,
    @SerialName("company_name") val companyName: String,
    val location: String,
    @SerialName("pay_per_hour") val payPerHour: Double,
    @SerialName("hours_per_week") val hoursPerWeek: Int,
    val description: String,
)

fun Job.toInsert() = JobInsert(
    employerId = employerId,
    title = title,
    companyName = companyName,
    location = location,
    payPerHour = payPerHour,
    hoursPerWeek = hoursPerWeek,
    description = description,
)

// ---------- applications ----------

@Serializable
data class ApplicationRow(
    val id: String,
    @SerialName("job_id") val jobId: String,
    @SerialName("student_id") val studentId: String,
    val status: String,
    @SerialName("applied_at") val appliedAt: Long,
    @SerialName("ai_match_percent") val aiMatchPercent: Int? = null,
    @SerialName("ai_suggested_status") val aiSuggestedStatus: String? = null,
    @SerialName("ai_reason") val aiReason: String? = null,
)

fun ApplicationRow.toDomain() = Application(
    id = id,
    jobId = jobId,
    studentId = studentId,
    status = ApplicationStatus.valueOf(status),
    appliedAt = appliedAt,
    aiMatchPercent = aiMatchPercent,
    aiSuggestedStatus = aiSuggestedStatus,
    aiReason = aiReason,
)

@Serializable
data class ApplicationInsert(
    @SerialName("job_id") val jobId: String,
    @SerialName("student_id") val studentId: String,
    val status: String,
    @SerialName("applied_at") val appliedAt: Long,
    // AI advice rides along with the insert (RLS lets students insert,
    // but only employers update - so the advice must be written here).
    @SerialName("ai_match_percent") val aiMatchPercent: Int? = null,
    @SerialName("ai_suggested_status") val aiSuggestedStatus: String? = null,
    @SerialName("ai_reason") val aiReason: String? = null,
)
