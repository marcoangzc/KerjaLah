package com.kerjalah.advisor

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// Tests for the two pure parts of the advisor: how the prompt is assembled
// and how the model's reply is validated. Neither makes a network call.
//
// These matter because both are defences, not conveniences: the prompt is what
// keeps user text from being read as instructions, and the parser is what keeps
// a malformed reply from violating a DB CHECK constraint and killing the whole
// application insert.
class GroqAdvisorTest {

    private val advisor = GroqAdvisor(HttpClient(CIO))

    private fun job(description: String = "Serve coffee.") = JobRow(
        id = "job-1",
        title = "Barista",
        companyName = "Kopi Co",
        location = "Near UM, KL",
        payPerHour = 10.0,
        hoursPerWeek = 12,
        description = description,
    )

    private fun profile(bio: String = "I like coffee.") = ProfileRow(
        id = "stu-1",
        role = "STUDENT",
        organization = "Universiti Malaya",
        bio = bio,
    )

    // ---------- prompt assembly ----------

    @Test
    fun `long job description and bio are truncated to 500 characters`() {
        val message = advisor.buildUserMessage(job("x".repeat(900)), profile("y".repeat(900)))

        // 500 chars + the "..." marker, not the original 900.
        assertTrue(message.contains("x".repeat(500) + "..."))
        assertFalse(message.contains("x".repeat(501)))
        assertTrue(message.contains("y".repeat(500) + "..."))
        assertFalse(message.contains("y".repeat(501)))
    }

    @Test
    fun `untrusted free text stays inside its delimiters`() {
        val message = advisor.buildUserMessage(job(), profile())

        assertTrue(message.contains("<job_description>"))
        assertTrue(message.contains("</job_description>"))
        assertTrue(message.contains("<student_bio>"))
        assertTrue(message.contains("</student_bio>"))
    }

    @Test
    fun `student name is never sent to the model`() {
        // ProfileRow has no name field at all, by design - this test pins that
        // decision so re-adding one for convenience trips a red test.
        val message = advisor.buildUserMessage(job(), profile())

        assertFalse(message.contains("name"))
        assertTrue(message.contains("student_organization: Universiti Malaya"))
    }

    @Test
    fun `blank free text becomes an explicit placeholder`() {
        val message = advisor.buildUserMessage(job(description = "  "), profile(bio = ""))

        assertTrue(message.contains("(no description provided)"))
        assertTrue(message.contains("(no bio provided)"))
    }

    // ---------- reply validation ----------

    @Test
    fun `a well formed reply parses`() {
        val result = advisor.parseAssessment(
            """{"matchPercent": 82, "suggestedStatus": "STRONG_MATCH", "reason": "Has barista experience."}""",
        )

        assertEquals(82, result?.matchPercent)
        assertEquals(AiSuggestedStatus.STRONG_MATCH, result?.suggestedStatus)
        assertEquals("Has barista experience.", result?.reason)
    }

    @Test
    fun `reasoning tags and code fences are stripped`() {
        val result = advisor.parseAssessment(
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
        // The DB CHECK allows 0-100 only; an insert of 150 would fail the row.
        assertEquals(100, advisor.parseAssessment(verdict(percent = 150))?.matchPercent)
        assertEquals(0, advisor.parseAssessment(verdict(percent = -20))?.matchPercent)
    }

    @Test
    fun `the old ACCEPTED vocabulary is rejected rather than stored`() {
        // Pre-migration models answered ACCEPTED/REJECTED. That vocabulary now
        // violates applications_ai_suggested_status_check, so it must become
        // "no advice" instead of a failed insert.
        assertNull(advisor.parseAssessment(verdict(status = "ACCEPTED")))
        assertNull(advisor.parseAssessment(verdict(status = "REJECTED")))
    }

    @Test
    fun `unknown or missing fields yield no advice`() {
        assertNull(advisor.parseAssessment(verdict(status = "MAYBE_LATER")))
        assertNull(advisor.parseAssessment("""{"suggestedStatus": "STRONG_MATCH"}"""))
        assertNull(advisor.parseAssessment("""{"matchPercent": "abc", "suggestedStatus": "WEAK_MATCH"}"""))
        assertNull(advisor.parseAssessment("not json at all"))
        assertNull(advisor.parseAssessment(""))
    }

    @Test
    fun `an over long reason is truncated to 300 characters`() {
        val result = advisor.parseAssessment(verdict(reason = "z".repeat(500)))

        assertEquals("z".repeat(300) + "...", result?.reason)
    }

    private fun verdict(
        percent: Int = 50,
        status: String = "POSSIBLE_MATCH",
        reason: String = "Fine.",
    ) = """{"matchPercent": $percent, "suggestedStatus": "$status", "reason": "$reason"}"""
}
