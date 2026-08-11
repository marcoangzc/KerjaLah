package com.kerjalah.app.navigation

// [A] All route strings live in ONE place.
// Why: screens never build route strings themselves;
// NavGraph is the only navigator (UDF: navigation events go up).
object Routes {
    // Entry flow (Module 1)
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val ROLE = "role"

    // Student side
    const val STUDENT_JOBS = "student/jobs"
    const val STUDENT_JOB_DETAIL = "student/job/{jobId}"
    const val STUDENT_APPLICATIONS = "student/applications"
    const val STUDENT_APPLICATION_DETAIL = "student/application/{appId}"
    const val STUDENT_PROFILE = "student/profile"
    const val STUDENT_PROFILE_EDIT = "student/profile/edit"

    // Employer side
    const val EMPLOYER_POSTINGS = "employer/postings"
    const val EMPLOYER_POST = "employer/post?jobId={jobId}" // optional query arg
    const val EMPLOYER_APPLICANTS = "employer/applicants/{jobId}"
    const val EMPLOYER_APPLICANT_DETAIL = "employer/applicant/{appId}"
    const val EMPLOYER_PROFILE = "employer/profile"
    const val EMPLOYER_PROFILE_EDIT = "employer/profile/edit"

    // Helpers: build a concrete route from an id (avoid typos everywhere).
    fun studentJobDetail(jobId: String) = "student/job/$jobId"
    fun studentApplicationDetail(appId: String) = "student/application/$appId"
    fun employerPost(jobId: String? = null) =
        if (jobId == null) "employer/post" else "employer/post?jobId=$jobId"
    fun employerApplicants(jobId: String) = "employer/applicants/$jobId"
    fun employerApplicantDetail(appId: String) = "employer/applicant/$appId"
}
