package com.kerjalah.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// Tests for the two pure parts of the AI advisor: how the prompt is assembled
// and how the model's reply is validated. Neither makes a network call.
//
// These matter because both are defences, not conveniences. The prompt is what
// keeps user-typed text from being read as instructions, and the parser is what
// keeps a malformed reply from violating a DB CHECK constraint and taking the
// whole application insert down with it.
class AiClientTest {

    private fun job(description: String = "Serve coffee.") = Job(
        id = "job-1",
        employerId = "emp-1",
        title = "Barista",
        companyName = "Kopi Co",
        location = "Near UM, KL",
        payPerHour = 10.0,
        hoursPerWeek = 12,
        description = description,
    )

    private fun student(bio: String = "I like coffee.") = User(
        id = "stu-1",
        role = UserRole.STUDENT,
        name = "Siti Nurhaliza",
        email = "siti@example.com",
        password = "",
        organization = "Universiti Malaya",
        bio = bio,
    )

    // ---------- prompt assembly ----------

    @Test
    fun `long job description and bio are truncated to 500 characters`() {
        val message = AiClient.buildUserMessage(job("x".repeat(900)), student("y".repeat(900)))

        // 500 chars plus the "..." marker, not the original 900.
        assertTrue(message.contains("x".repeat(500) + "..."))
        assertFalse(message.contains("x".repeat(501)))
        assertTrue(message.contains("y".repeat(500) + "..."))
        assertFalse(message.contains("y".repeat(501)))
    }

    @Test
    fun `untrusted free text stays inside its delimiters`() {
        val message = AiClient.buildUserMessage(job(), student())

        assertTrue(message.contains("<job_description>"))
        assertTrue(message.contains("</job_description>"))
        assertTrue(message.contains("<student_bio>"))
        assertTrue(message.contains("</student_bio>"))
    }

    @Test
    fun `the student's name and email never reach the model`() {
        // The model does not need either to score fit, so they are left out
        // of a third-party API call. This test pins that decision.
        val message = AiClient.buildUserMessage(job(), student())

        assertFalse(message.contains("Siti"))
        assertFalse(message.contains("siti@example.com"))
        assertTrue(message.contains("student_organization: Universiti Malaya"))
    }

    @Test
    fun `blank free text becomes an explicit placeholder`() {
        val message = AiClient.buildUserMessage(job(description = "  "), student(bio = ""))

        assertTrue(message.contains("(no description provided)"))
        assertTrue(message.contains("(no bio provided)"))
    }

    // ---------- reply validation ----------

    @Test
    fun `a well formed reply parses`() {
        val result = AiClient.parseAssessment(
            """{"matchPercent": 82, "suggestedStatus": "STRONG_MATCH", "reason": "Has barista experience."}""",
        )

        assertEquals(82, result?.matchPercent)
        assertEquals(AiSuggestedStatus.STRONG_MATCH, result?.suggestedStatus)
        assertEquals("Has barista experience.", result?.reason)
    }

    @Test
    fun `reasoning tags and code fences are stripped`() {
        val result = AiClient.parseAssessment(
            """
            <think>Let me weigh the location fit.</think>
            ```json
            {"matchPercent": 55, "suggestedStatus": "POSSIBLE_MATCH", "reason": "Some overlap."}
            ```
            """.trimIndent(),
        )

        assertEquals(55, result?.matchPercent)
        assertEquals(AiSuggestedStatus.POSSIBLE_MATCH, result?.suggestedStatus)
    }

    @Test
    fun `out of range percentages are clamped into 0 to 100`() {
        // The DB CHECK allows 0-100 only; inserting 150 would fail the row.
        assertEquals(100, AiClient.parseAssessment(verdict(percent = 150))?.matchPercent)
        assertEquals(0, AiClient.parseAssessment(verdict(percent = -20))?.matchPercent)
    }

    @Test
    fun `the old ACCEPTED vocabulary is rejected rather than stored`() {
        // Older prompts asked for ACCEPTED/REJECTED. That vocabulary now
        // violates applications_ai_suggested_status_check, so it has to become
        // "no advice" rather than a failed insert.
        assertNull(AiClient.parseAssessment(verdict(status = "ACCEPTED")))
        assertNull(AiClient.parseAssessment(verdict(status = "REJECTED")))
    }

    @Test
    fun `unknown or missing fields yield no advice`() {
        assertNull(AiClient.parseAssessment(verdict(status = "MAYBE_LATER")))
        assertNull(AiClient.parseAssessment("""{"suggestedStatus": "STRONG_MATCH"}"""))
        assertNull(AiClient.parseAssessment("""{"matchPercent": "abc", "suggestedStatus": "WEAK_MATCH"}"""))
        assertNull(AiClient.parseAssessment("not json at all"))
        assertNull(AiClient.parseAssessment(""))
    }

    @Test
    fun `an over long reason is truncated to 300 characters`() {
        val result = AiClient.parseAssessment(verdict(reason = "z".repeat(500)))

        assertEquals("z".repeat(300) + "...", result?.reason)
    }

    private fun verdict(
        percent: Int = 50,
        status: String = "POSSIBLE_MATCH",
        reason: String = "Fine.",
    ) = """{"matchPercent": $percent, "suggestedStatus": "$status", "reason": "$reason"}"""
}
