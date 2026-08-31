package com.kerjalah.app.data

// [B] Module 1 - user roles. One app, two experiences.
enum class UserRole { STUDENT, EMPLOYER }

// [B] Module 1 - User data model (Data Layer).
// Maps to the Supabase "profiles" table, plus the session for the email.
data class User(
    val id: String,
    val role: UserRole,
    val name: String,
    // NOT a profiles column: auth.users owns the address, so this is filled
    // from the session and is therefore only ever populated for the logged-in
    // user. Other people's User objects carry "" here - an employer looking at
    // an applicant has no reason to see their email.
    val email: String,
    val password: String,     // mock only! Supabase Auth will own this later
    val organization: String, // university (student) or company (employer)
    val bio: String,
)
