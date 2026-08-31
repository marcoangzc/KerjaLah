package com.kerjalah.app.data

import kotlin.time.Instant

// [B] Module 3 - application status. The whole status flow:
// student applies (PENDING) -> employer decides (ACCEPTED / REJECTED).
enum class ApplicationStatus { PENDING, ACCEPTED, REJECTED }

// [B] Module 3 (AI phase) - what the advisor says about the FIT.
// Deliberately different words from ApplicationStatus: the AI describes a
// match, it never decides an outcome. Mirrors the DB CHECK constraint
// `applications_ai_suggested_status_check`.
enum class AiSuggestedStatus {
    STRONG_MATCH,
    POSSIBLE_MATCH,
    WEAK_MATCH,
    ;

    companion object {
        // Unknown/absent values become null - a model that invents a new label
        // must not crash the applicant list.
        fun fromRaw(raw: String?): AiSuggestedStatus? =
            raw?.let { value -> entries.find { it.name == value } }
    }
}

// [B] Module 3 - Application data model (Data Layer).
// Links a User (student) to a Job -> User applies Job = Application.
// Maps to the Supabase "applications" table.
data class Application(
    val id: String,
    val jobId: String,
    val studentId: String,
    val status: ApplicationStatus,
    // Stamped by Postgres (`default now()`), never by the phone: a device with
    // a skewed clock used to be able to file an application dated next year.
    val appliedAt: Instant,
    // AI advisor fields, written server-side by the :advisor Ktor server.
    // AI only SUGGESTS - the employer always makes the real decision.
    val aiMatchPercent: Int? = null,
    val aiSuggestedStatus: AiSuggestedStatus? = null,
    val aiReason: String? = null,
)
