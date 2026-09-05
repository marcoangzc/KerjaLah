package com.kerjalah.app.data

import kotlinx.coroutines.CancellationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

// Tests for the boundary that turns exceptions into sentences.
//
// This is worth testing because it is a defence, not a convenience: it is the
// single place that decides whether a user sees "Check your connection" or a
// PostgREST error naming one of our columns.
class AppErrorTest {

    // ---------- classification ----------

    @Test
    fun `no DNS is reported as being offline`() {
        assertEquals(AppError.Offline, UnknownHostException("api.supabase.co").toAppError())
    }

    @Test
    fun `a socket timeout is reported as a timeout, not as being offline`() {
        // SocketTimeoutException extends IOException, so this also pins the
        // ordering inside toAppError: a broader branch must not swallow it.
        assertEquals(AppError.Timeout, SocketTimeoutException("read timed out").toAppError())
    }

    @Test
    fun `any other IO failure is reported as being offline`() {
        assertEquals(AppError.Offline, IOException("Connection reset by peer").toAppError())
    }

    @Test
    fun `an unrecognised failure falls back to the generic message`() {
        assertEquals(AppError.Unknown, IllegalStateException("boom").toAppError())
    }

    // ---------- the actual promise this file makes ----------

    @Test
    fun `no user-facing message leaks technical vocabulary`() {
        val leaks = listOf(
            "exception", "RestException", "null", "SQL", "postgres", "RLS",
            "Ktor", "HTTP", "stack", "token", "supabase", "auth.uid",
        )
        AppError.entries.forEach { error ->
            leaks.forEach { word ->
                assertFalse(
                    "${error.name} leaks \"$word\": ${error.message}",
                    error.message.contains(word, ignoreCase = true),
                )
            }
        }
    }

    @Test
    fun `every message is a finished sentence a person can read`() {
        AppError.entries.forEach { error ->
            assertTrue("${error.name} is too terse", error.message.length > 15)
            assertTrue(
                "${error.name} does not end as a sentence",
                error.message.endsWith("."),
            )
        }
    }

    // ---------- structured concurrency ----------

    @Test
    fun `resultOf captures ordinary failures`() {
        val result = resultOf { error("nope") }
        assertTrue(result.isFailure)
    }

    @Test(expected = CancellationException::class)
    fun `resultOf lets coroutine cancellation through`() {
        // runCatching would have swallowed this, and a cancelled screen would
        // have looked exactly like a failed request.
        resultOf { throw CancellationException("screen left") }
    }
}
