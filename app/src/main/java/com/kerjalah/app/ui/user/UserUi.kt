package com.kerjalah.app.ui.user

import com.kerjalah.app.data.data.User
import com.kerjalah.app.data.data.UserRole

// [B] UI model for the profile screens. Ready-to-show text only.
data class ProfileUi(
    val name: String,
    val email: String,
    val roleLabel: String, // "Student" / "Employer"
    val orgLabel: String,  // "University" / "Company"
    val orgValue: String,
    val bio: String,
)

// [B] Transform: Data model -> UI model (UDF "toUi" step).
fun User.toUi() = ProfileUi(
    name = name,
    email = email,
    roleLabel = if (role == UserRole.STUDENT) "Student" else "Employer",
    orgLabel = if (role == UserRole.STUDENT) "University" else "Company",
    orgValue = organization,
    bio = bio,
)
