package com.example.util

import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import java.security.MessageDigest

fun hashPassword(password: String): String {
    val bytes = MessageDigest.getInstance("SHA-256").digest("motion_iq_salt_$password".toByteArray())
    return bytes.joinToString("") { "%02x".format(it) }
}

fun isApiKeyOrConfigError(e: Throwable?): Boolean {
    if (e == null) return false
    val allText = buildString {
        append(e.javaClass.name).append(" ")
        append(e.message ?: "").append(" ")
        append(e.localizedMessage ?: "").append(" ")
        var cause = e.cause
        while (cause != null) {
            append(cause.javaClass.name).append(" ")
            append(cause.message ?: "").append(" ")
            append(cause.localizedMessage ?: "").append(" ")
            cause = cause.cause
        }
    }
    return allText.contains("API key", ignoreCase = true) ||
           allText.contains("API_KEY", ignoreCase = true) ||
           allText.contains("Identity Toolkit", ignoreCase = true) ||
           allText.contains("Recaptcha", ignoreCase = true) ||
           allText.contains("internal error", ignoreCase = true) ||
           allText.contains("blocked", ignoreCase = true) ||
           allText.contains("No credential", ignoreCase = true) ||
           allText.contains("developer_error", ignoreCase = true) ||
           allText.contains("INVALID_ARGUMENT", ignoreCase = true) ||
           allText.contains("API_KEY_INVALID", ignoreCase = true) ||
           allText.contains("PROJECT_NOT_FOUND", ignoreCase = true) ||
           allText.contains("CONFIGURATION_NOT_FOUND", ignoreCase = true) ||
           allText.contains("SERVICE_DISABLED", ignoreCase = true) ||
           allText.contains("BILLING_NOT_ENABLED", ignoreCase = true) ||
           allText.contains("PERMISSION_DENIED", ignoreCase = true) ||
           allText.contains("UNAUTHENTICATED", ignoreCase = true) ||
           allText.contains("ERROR_INTERNAL_ERROR", ignoreCase = true) ||
           allText.contains("An internal error has occurred", ignoreCase = true) ||
           allText.contains("OPERATION_NOT_ALLOWED", ignoreCase = true) ||
           allText.contains("operation is not allowed", ignoreCase = true) ||
           allText.contains("sign-in provider is disabled", ignoreCase = true) ||
           allText.contains("signInWithPassword", ignoreCase = true) ||
           allText.contains("signInWithEmailAndPassword", ignoreCase = true)
}

fun isValidEmailAddress(email: String): Boolean {
    val trimmed = email.trim().lowercase()
    if (trimmed.isBlank()) return false
    val regex = "^[a-z0-9._%+-]+@[a-z0-9.-]+\\.[a-z]{2,}$".toRegex()
    return regex.matches(trimmed)
}

fun isValidGmailAddress(email: String): Boolean {
    return isValidEmailAddress(email)
}

fun formatFriendlyAuthError(e: Throwable?): String {
    if (e == null) return "Authentication failed. Please try again."
    val msg = e.localizedMessage ?: e.message ?: ""
    val errCode = (e as? FirebaseAuthException)?.errorCode ?: ""

    return when {
        // invalid-credential / wrong-password
        errCode == "ERROR_WRONG_PASSWORD" ||
        errCode == "ERROR_INVALID_CREDENTIAL" ||
        errCode.contains("wrong-password", ignoreCase = true) ||
        errCode.contains("invalid-credential", ignoreCase = true) ||
        msg.contains("wrong-password", ignoreCase = true) ||
        msg.contains("invalid-credential", ignoreCase = true) ||
        msg.contains("invalid credential", ignoreCase = true) ||
        e is FirebaseAuthInvalidCredentialsException ->
            "Invalid email or password."

        // user-not-found
        errCode == "ERROR_USER_NOT_FOUND" ||
        errCode.contains("user-not-found", ignoreCase = true) ||
        errCode.contains("USER_NOT_FOUND", ignoreCase = true) ||
        msg.contains("user-not-found", ignoreCase = true) ||
        msg.contains("user_not_found", ignoreCase = true) ||
        msg.contains("USER_NOT_FOUND", ignoreCase = true) ||
        e is FirebaseAuthInvalidUserException ->
            "No account found with this email."

        // email-already-in-use
        errCode == "ERROR_EMAIL_ALREADY_IN_USE" ||
        errCode.contains("email-already-in-use", ignoreCase = true) ||
        errCode.contains("EMAIL_EXISTS", ignoreCase = true) ||
        msg.contains("email-already-in-use", ignoreCase = true) ||
        msg.contains("EMAIL_EXISTS", ignoreCase = true) ||
        msg.contains("already in use", ignoreCase = true) ||
        msg.contains("already exists", ignoreCase = true) ||
        e is FirebaseAuthUserCollisionException ->
            "An account already exists with this email. Please sign in."

        // weak-password
        errCode == "ERROR_WEAK_PASSWORD" ||
        errCode.contains("weak-password", ignoreCase = true) ||
        errCode.contains("WEAK_PASSWORD", ignoreCase = true) ||
        msg.contains("weak-password", ignoreCase = true) ||
        msg.contains("weak_password", ignoreCase = true) ||
        msg.contains("WEAK_PASSWORD", ignoreCase = true) ||
        e is FirebaseAuthWeakPasswordException ->
            "Password must contain at least 8 characters."

        // invalid-email
        errCode == "ERROR_INVALID_EMAIL" ||
        errCode.contains("invalid-email", ignoreCase = true) ||
        msg.contains("badly formatted", ignoreCase = true) ->
            "Please enter a valid email address."

        // network-request-failed
        errCode == "ERROR_NETWORK_REQUEST_FAILED" ||
        errCode.contains("network-request-failed", ignoreCase = true) ||
        msg.contains("network", ignoreCase = true) ||
        msg.contains("connection", ignoreCase = true) ||
        msg.contains("timeout", ignoreCase = true) ->
            "Internet connection unavailable. Please check your connection."

        // too-many-requests
        errCode == "ERROR_TOO_MANY_REQUESTS" ||
        errCode.contains("too-many-requests", ignoreCase = true) ||
        msg.contains("too-many-requests", ignoreCase = true) ->
            "Too many login attempts. Please try again later."

        // user-disabled
        errCode == "ERROR_USER_DISABLED" ||
        errCode.contains("user-disabled", ignoreCase = true) ||
        errCode.contains("USER_DISABLED", ignoreCase = true) ||
        msg.contains("user-disabled", ignoreCase = true) ||
        msg.contains("user_disabled", ignoreCase = true) ||
        msg.contains("user has been disabled", ignoreCase = true) ||
        msg.contains("user is disabled", ignoreCase = true) ->
            "This account has been disabled."

        // account-exists-with-different-credential
        errCode == "ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL" ||
        errCode.contains("account-exists-with-different-credential", ignoreCase = true) ||
        msg.contains("account-exists-with-different-credential", ignoreCase = true) ->
            "This email is already registered with another sign-in method."

        // operation-not-allowed
        errCode == "ERROR_OPERATION_NOT_ALLOWED" ||
        errCode.contains("operation-not-allowed", ignoreCase = true) ||
        msg.contains("operation-not-allowed", ignoreCase = true) ||
        msg.contains("OPERATION_NOT_ALLOWED", ignoreCase = true) ||
        msg.contains("sign-in provider is disabled", ignoreCase = true) ||
        msg.contains("operation is not allowed", ignoreCase = true) ->
            "Email/password sign-in is currently disabled."

        else ->
            "Authentication failed. Please try again."
    }
}
