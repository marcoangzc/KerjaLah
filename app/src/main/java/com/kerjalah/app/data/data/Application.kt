package com.kerjalah.app.data.data

// [B] Module 3 - application status. The whole status flow:
// student applies (PENDING) -> employer decides (ACCEPTED / REJECTED).
enum class ApplicationStatus { PENDING, ACCEPTED, REJECTED }

// [B] Module 3 - Application data model (Data Layer).
// Links a User (student) to a Job -> User applies Job = Application.
// Later this maps to the Supabase "applications" table.
data class Application(
    val id: String,
    val jobId: String,
    val studentId: String,
    val status: ApplicationStatus,
    val appliedAt: Long, // epoch millis, used to sort newest first
    // AI advisor fields (filled by Gemini in the last phase).
    // AI only SUGGESTS - the employer always makes the real decision.
    val aiMatchPercent: Int? = null,
    val aiSuggestedStatus: String? = null,
    val aiReason: String? = null,
)
