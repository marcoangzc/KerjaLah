package com.kerjalah.app.data.data

// [B] Module 1 - user roles. One app, two experiences.
enum class UserRole { STUDENT, EMPLOYER }

// [B] Module 1 - User data model (Data Layer).
// Later this maps to the Supabase "profiles" table
// (auth fields move into Supabase Auth; password disappears).
data class User(
    val id: String,
    val role: UserRole,
    val name: String,
    val email: String,
    val password: String,     // mock only! Supabase Auth will own this later
    val organization: String, // university (student) or company (employer)
    val bio: String,
)
