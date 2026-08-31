package com.kerjalah.app.data

// [B] Bridge between Module 1 (auth) and Modules 2/3.
// Other modules keep calling these properties unchanged;
// the values come from the real mock session.
object CurrentUser {
    val EMPLOYER_ID: String
        get() = UserRepository.currentUser.value
            ?.takeIf { it.role == UserRole.EMPLOYER }
            ?.id
            ?: "emp-1" // safe fallback for previews / not-logged-in demos

    val STUDENT_ID: String
        get() = UserRepository.currentUser.value
            ?.takeIf { it.role == UserRole.STUDENT }
            ?.id
            ?: "stu-1" // safe fallback for previews / not-logged-in demos
}
