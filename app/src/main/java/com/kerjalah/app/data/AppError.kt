package com.kerjalah.app.data

import android.util.Log
import io.github.jan.supabase.auth.exception.AuthErrorCode
import io.github.jan.supabase.auth.exception.AuthRestException
import io.github.jan.supabase.exceptions.RestException
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import kotlinx.coroutines.CancellationException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

// [B] The ONE place where a Throwable becomes something a person can read.
//
// Why this file exists: screens used to render `e.message`, which meant a
// student could be shown "RestException ... new row violates row-level security
// policy for table \"applications\"" or a raw Ktor timeout dump. That text is
// useless to them, leaks our schema, and is not English anyone asked for.
//
// The rule now: a Throwable NEVER leaves the data layer. Repositories return
// an Outcome carrying an AppError, so a ViewModel literally cannot forward a
// stack trace to the UI - the type system does not let it. The technical
// detail still exists; it goes to Logcat, where it belongs.
enum class AppError(val message: String) {

    // --- Connectivity ---
    Offline("You appear to be offline. Check your connection and try again."),
    Timeout("The server is taking too long to respond. Please try again."),
    ServiceUnavailable("KerjaLah can't reach the server right now. Please try again in a moment."),
    RateLimited("Too many attempts. Please wait a minute and try again."),

    // --- Sign in / sign up ---
    InvalidCredentials("That email and password don't match an account."),
    EmailTaken("That email already has an account. Log in instead."),
    InvalidEmail("Please enter a valid email address."),
    WeakPassword("Please choose a stronger password - at least 6 characters."),
    EmailNotConfirmed("Please confirm your email address, then log in."),
    SignUpDisabled("New accounts are turned off at the moment."),
    SessionExpired("Your session has expired. Please log in again."),
    RegistrationExpired("Your sign-up details are no longer available. Please enter them again."),
    ProfileNotReady("Your account was created, but your profile isn't ready yet. Please log in again in a moment."),

    // --- Data access ---
    NotAllowed("You don't have permission to do that."),
    NotFound("That item is no longer available."),
    Conflict("Someone changed this before you. Refresh and try again."),

    // --- Anything we did not anticipate ---
    Unknown("Something went wrong. Please try again."),
}

// [B] Result of any repository call that can fail.
// Failure carries an AppError, never a Throwable - that is what keeps raw
// exception text out of the UI by construction rather than by discipline.
sealed interface Outcome<out T> {
    data class Success<T>(val value: T) : Outcome<T>
    data class Failure(val error: AppError) : Outcome<Nothing>
}

/** Shorthand for the very common "it worked, there is nothing to return". */
val Ok: Outcome<Unit> = Outcome.Success(Unit)

fun <T> Outcome<T>.valueOrNull(): T? = (this as? Outcome.Success)?.value

fun Outcome<*>.errorOrNull(): AppError? = (this as? Outcome.Failure)?.error

val Outcome<*>.succeeded: Boolean get() = this is Outcome.Success

// [B] runCatching, minus its one real flaw: it also swallows
// CancellationException, which quietly breaks structured concurrency (a
// cancelled screen would look like a failed request). Coroutine cancellation
// is rethrown; everything else becomes a Result we can map.
internal inline fun <T> resultOf(block: () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (throwable: Throwable) {
        Result.failure(throwable)
    }

// [B] Log the technical truth, return the human sentence.
// Call this at the boundary so no caller is ever tempted to touch `message`.
internal fun Throwable.logged(tag: String, what: String): AppError {
    Log.e(tag, what, this)
    return toAppError()
}

/** Classify a failure. Ordering matters: the timeout types all extend IOException. */
fun Throwable.toAppError(): AppError = when (this) {
    is AuthRestException -> errorCode.toAppError()
    is RestException -> httpStatusToAppError(statusCode)
    is HttpRequestTimeoutException,
    is ConnectTimeoutException,
    is SocketTimeoutException,
    -> AppError.Timeout
    is UnknownHostException -> AppError.Offline
    // Covers supabase-kt's HttpRequestException, ConnectException, SSL faults,
    // and anything else the socket layer throws when the network is unhappy.
    is IOException -> AppError.Offline
    else -> AppError.Unknown
}

// Supabase Auth returns a documented error code; prefer it over the message,
// which is English written by GoTrue rather than by us.
private fun AuthErrorCode?.toAppError(): AppError = when (this) {
    AuthErrorCode.InvalidCredentials,
    AuthErrorCode.UserNotFound,
    AuthErrorCode.BadCodeVerifier,
    -> AppError.InvalidCredentials

    AuthErrorCode.EmailExists,
    AuthErrorCode.UserAlreadyExists,
    AuthErrorCode.IdentityAlreadyExists,
    -> AppError.EmailTaken

    AuthErrorCode.EmailAddressInvalid,
    AuthErrorCode.ValidationFailed,
    -> AppError.InvalidEmail

    AuthErrorCode.WeakPassword -> AppError.WeakPassword

    AuthErrorCode.EmailNotConfirmed,
    AuthErrorCode.ProviderEmailNeedsVerification,
    -> AppError.EmailNotConfirmed

    AuthErrorCode.SignupDisabled,
    AuthErrorCode.EmailProviderDisabled,
    AuthErrorCode.ProviderDisabled,
    AuthErrorCode.EmailAddressNotAuthorized,
    -> AppError.SignUpDisabled

    AuthErrorCode.OverRequestRateLimit,
    AuthErrorCode.OverEmailSendRateLimit,
    -> AppError.RateLimited

    AuthErrorCode.RequestTimeout,
    AuthErrorCode.HookTimeout,
    AuthErrorCode.HookTimeoutAfterRetry,
    -> AppError.Timeout

    AuthErrorCode.SessionNotFound,
    AuthErrorCode.SessionExpired,
    AuthErrorCode.RefreshTokenNotFound,
    AuthErrorCode.RefreshTokenAlreadyUsed,
    AuthErrorCode.BadJwt,
    -> AppError.SessionExpired

    AuthErrorCode.UserBanned,
    AuthErrorCode.NotAdmin,
    AuthErrorCode.NoAuthorization,
    -> AppError.NotAllowed

    AuthErrorCode.Conflict -> AppError.Conflict

    else -> AppError.Unknown
}

// HTTP status -> sentence. Used for PostgREST failures (including RLS
// rejections, which arrive as 401/403) and for Groq's own error responses.
internal fun httpStatusToAppError(status: Int): AppError = when (status) {
    401 -> AppError.SessionExpired
    403 -> AppError.NotAllowed
    404 -> AppError.NotFound
    409 -> AppError.Conflict
    429 -> AppError.RateLimited
    in 500..599 -> AppError.ServiceUnavailable
    else -> AppError.Unknown
}
