package com.kerjalah.app.ui.application

import com.kerjalah.app.data.data.Application
import com.kerjalah.app.data.data.ApplicationStatus
import com.kerjalah.app.data.data.Job
import com.kerjalah.app.data.data.User

// [B] UI model for the STUDENT side: one of my applications.
data class ApplicationUi(
    val id: String,
    val jobId: String,
    val jobTitle: String,
    val companyName: String,
    val payText: String,
    val status: ApplicationStatus,
    val canWithdraw: Boolean, // only pending applications can be withdrawn
)

// [B] UI model for the EMPLOYER side: one applicant for my job.
// AI fields are employer-only by design: students never see them.
data class ApplicantUi(
    val id: String, // application id
    val studentName: String,
    val studentOrg: String, // university
    val studentBio: String,
    val jobTitle: String,
    val status: ApplicationStatus,
    val aiMatchPercent: Int? = null,     // null = AI had no advice
    val aiSuggestedStatus: String? = null,
    val aiReason: String? = null,
)

// [B] Transform: Application + its Job -> student-facing UI model.
// The join happens in the ViewModel; formatting lives here (UDF toUi).
fun Application.toStudentUi(job: Job?) = ApplicationUi(
    id = id,
    jobId = jobId,
    jobTitle = job?.title ?: "Job no longer available",
    companyName = job?.companyName ?: "",
    payText = job?.let { "RM %.2f / hour".format(it.payPerHour) } ?: "",
    status = status,
    canWithdraw = status == ApplicationStatus.PENDING,
)

// [B] Transform: Application + its student + its Job -> employer-facing model.
fun Application.toApplicantUi(student: User?, job: Job?) = ApplicantUi(
    id = id,
    studentName = student?.name ?: "Account deleted",
    studentOrg = student?.organization ?: "",
    studentBio = student?.bio ?: "",
    jobTitle = job?.title ?: "Job no longer available",
    status = status,
    aiMatchPercent = aiMatchPercent,
    aiSuggestedStatus = aiSuggestedStatus,
    aiReason = aiReason,
)
