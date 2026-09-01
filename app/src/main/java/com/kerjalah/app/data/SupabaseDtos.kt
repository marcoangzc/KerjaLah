package com.kerjalah.app.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Instant

// [A] DTOs: exact shapes of the Supabase table rows.
// Why separate from the domain models: table columns are snake_case and
// insert rows have no id (the database generates it). Mapping here keeps
// the domain models - and everything above them - unchanged (UDF layering).

// ---------- profiles ----------

// No email column: auth.users owns the address (see supabase_migration_01.sql).
// A second copy could drift, and it was the most sensitive field exposed by the
// old "every signed-in user can read every profile" policy.
@Serializable
data class ProfileRow(
    val id: String,
    val role: String,
    val name: String,
    val organization: String = "",
    val bio: String = "",
)

// email comes from the auth session, not from this row - callers that only
// have a profile (e.g. the employer looking at an applicant) pass nothing and
// simply have no address to show.
fun ProfileRow.toDomain(email: String = "") = User(
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

// Insert payload. Note what is NOT here: applied_at. The database stamps it
// with `default now()` and the client has no grant to write it, so a phone
// with a skewed clock can no longer file an application dated next year.
//
// The ai_* columns ride along with the insert because RLS lets a student
// INSERT their own row but never UPDATE it - so the advice has to arrive with
// the row or not at all.
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
data class ApplicationRow(
    val id: String,
    @SerialName("job_id") val jobId: String,
    @SerialName("student_id") val studentId: String,
    val status: String,
    // timestamptz on the wire is an ISO-8601 string; parsed in toDomain() so
    // the DTO stays a plain mirror of the row and needs no custom serializer.
    @SerialName("applied_at") val appliedAt: String,
    @SerialName("ai_match_percent") val aiMatchPercent: Int? = null,
    @SerialName("ai_suggested_status") val aiSuggestedStatus: String? = null,
    @SerialName("ai_reason") val aiReason: String? = null,
)

fun ApplicationRow.toDomain() = Application(
    id = id,
    jobId = jobId,
    studentId = studentId,
    status = ApplicationStatus.valueOf(status),
    appliedAt = parseTimestamptz(appliedAt),
    aiMatchPercent = aiMatchPercent,
    aiSuggestedStatus = AiSuggestedStatus.fromRaw(aiSuggestedStatus),
    aiReason = aiReason,
)

// PostgREST renders timestamptz as e.g. "2026-08-30T09:15:00.123456+00:00".
// Instant.parse handles the offset form; DISTANT_PAST keeps an unparseable
// value at the bottom of a newest-first list instead of crashing it.
private fun parseTimestamptz(raw: String): Instant =
    runCatching { Instant.parse(raw) }.getOrDefault(Instant.DISTANT_PAST)
