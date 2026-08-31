package com.example.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.util.Base64
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialCustomException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialProviderConfigurationException
import androidx.credentials.exceptions.GetCredentialUnsupportedException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.tasks.Task
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import org.json.JSONObject
import java.security.MessageDigest
import java.util.UUID

/**
 * Result model encapsulating Google User Profile and Auth Token data.
 */
data class GoogleAuthUser(
    val idToken: String,
    val userId: String,
    val email: String,
    val displayName: String,
    val photoUrl: String? = null
)

/**
 * Production-ready Manager for Android Google Sign-In authentication.
 * Supports modern Jetpack Credential Manager with automatic fallback to GoogleSignInClient Intent.
 */
object GoogleAuthManager {

    private const val TAG = "MotionIQ_Auth"
    private const val DEFAULT_FALLBACK_CLIENT_ID = "759280009859-ceglqtg85m6pi07d7vquhogri4t3q9s4.apps.googleusercontent.com"

    /**
     * Unwraps a Context to find its parent Activity if available.
     */
    fun findActivity(context: Context): Activity? {
        var ctx = context
        while (ctx is ContextWrapper) {
            if (ctx is Activity) return ctx
            ctx = ctx.baseContext
        }
        return null
    }

    /**
     * Checks whether Google Play Services is installed and up-to-date on the device.
     */
    fun isGooglePlayServicesAvailable(context: Context): Boolean {
        val availability = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context)
        val isAvailable = availability == ConnectionResult.SUCCESS
        Log.d(TAG, "Google Play Services check: availabilityCode=$availability (SUCCESS=${isAvailable})")
        return isAvailable
    }

    /**
     * Resolves the Web Client ID from resources, environment, or default fallback.
     */
    fun getServerClientId(context: Context): String {
        return try {
            val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
            if (resId != 0) {
                val id = context.getString(resId)
                if (id.isNotBlank()) {
                    Log.d(TAG, "Resolved serverClientId from string resources (resId=$resId)")
                    id
                } else {
                    DEFAULT_FALLBACK_CLIENT_ID
                }
            } else {
                DEFAULT_FALLBACK_CLIENT_ID
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to resolve default_web_client_id from string resources: ${e.message}")
            DEFAULT_FALLBACK_CLIENT_ID
        }
    }

    /**
     * Generates a cryptographically secure SHA-256 hashed nonce string.
     */
    fun generateHashedNonce(): String {
        val rawNonce = UUID.randomUUID().toString()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(rawNonce.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    /**
     * Creates a configured GoogleSignInClient instance for Intent-based authentication.
     */
    fun getGoogleSignInClient(activity: Activity): GoogleSignInClient {
        val serverClientId = getServerClientId(activity)
        Log.d(TAG, "Initializing GoogleSignInClient with serverClientId: ${serverClientId.take(15)}...")

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(serverClientId)
            .requestEmail()
            .requestProfile()
            .build()

        return GoogleSignIn.getClient(activity, gso)
    }

    /**
     * Initiates the Google Sign-In flow.
     * Checks Google Play Services, then invokes Credential Manager or falls back to Intent launcher.
     */
    suspend fun signIn(
        context: Context,
        onSuccess: (GoogleAuthUser) -> Unit,
        onError: (String) -> Unit,
        onCancelled: () -> Unit,
        onLaunchIntentPicker: (Intent) -> Unit
    ) {
        val activity = findActivity(context) ?: (context as? Activity)
        Log.d(TAG, "signIn() invoked. Context is activity: ${activity != null}")

        // 1. Validate Google Play Services availability
        if (!isGooglePlayServicesAvailable(context)) {
            Log.w(TAG, "Google Play Services check failed. Reporting requirement to user.")
            onError("Google Play Services are required for Google Sign-In.")
            return
        }

        val clientId = getServerClientId(context)
        Log.d(TAG, "Starting Google Sign-In flow with Web Client ID: ${clientId.take(15)}...")

        if (activity == null) {
            Log.e(TAG, "No Activity context found to present Google account chooser.")
            onError("Authentication error: Activity context required to show account chooser.")
            return
        }

        val hashedNonce = generateHashedNonce()

        // 2. Try modern Android Jetpack Credential Manager
        try {
            val credentialManager = CredentialManager.create(activity)

            val signInOption = GetSignInWithGoogleOption.Builder(clientId)
                .setNonce(hashedNonce)
                .build()

            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setAutoSelectEnabled(false)
                .setServerClientId(clientId)
                .setNonce(hashedNonce)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(signInOption)
                .addCredentialOption(googleIdOption)
                .build()

            Log.d(TAG, "Requesting credential from Jetpack CredentialManager...")
            val response = credentialManager.getCredential(
                request = request,
                context = activity
            )

            val credential = response.credential
            Log.d(TAG, "Received credential type: ${credential.type}")

            if (credential is CustomCredential &&
                (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL ||
                 credential.type == "com.google.android.libraries.identity.googleid.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL")
            ) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken
                var email = googleIdTokenCredential.id
                var displayName = googleIdTokenCredential.displayName ?: ""
                val photoUrl = googleIdTokenCredential.profilePictureUri?.toString()

                // Supplement missing profile fields from JWT payload
                if (displayName.isBlank() || email.isBlank()) {
                    val jwtPayload = parseJwtPayload(idToken)
                    if (jwtPayload != null) {
                        if (email.isBlank()) {
                            email = jwtPayload.optString("email", "")
                        }
                        if (displayName.isBlank()) {
                            displayName = jwtPayload.optString("name", "")
                            if (displayName.isBlank()) {
                                val given = jwtPayload.optString("given_name", "")
                                val family = jwtPayload.optString("family_name", "")
                                displayName = "$given $family".trim()
                            }
                        }
                    }
                }

                if (displayName.isBlank()) {
                    displayName = if (email.contains("@")) {
                        email.substringBefore("@").replaceFirstChar { it.uppercase() }
                    } else {
                        "MotionIQ Athlete"
                    }
                }

                val stableUserId = if (email.isNotBlank()) {
                    "google_" + kotlin.math.abs(email.lowercase().hashCode())
                } else {
                    "google_" + kotlin.math.abs(idToken.take(20).hashCode())
                }

                Log.d(TAG, "CredentialManager sign-in succeeded for email: $email, UID: $stableUserId")
                val user = GoogleAuthUser(
                    idToken = idToken,
                    userId = stableUserId,
                    email = email,
                    displayName = displayName,
                    photoUrl = photoUrl
                )
                onSuccess(user)
                return
            } else {
                Log.w(TAG, "Unexpected credential type (${credential.type}). Falling back to GoogleSignIn intent.")
                launchIntentChooser(activity, onLaunchIntentPicker, onError)
            }

        } catch (e: GetCredentialCancellationException) {
            Log.d(TAG, "Google Sign-In cancelled by user.")
            onCancelled()
        } catch (e: NoCredentialException) {
            Log.d(TAG, "CredentialManager returned NoCredentialException. Invoking GoogleSignIn Intent account chooser...")
            launchIntentChooser(activity, onLaunchIntentPicker, onError)
        } catch (e: GetCredentialProviderConfigurationException) {
            Log.w(TAG, "CredentialManager configuration issue: ${e.message}. Falling back to GoogleSignIn Intent...")
            launchIntentChooser(activity, onLaunchIntentPicker, onError)
        } catch (e: GetCredentialUnsupportedException) {
            Log.w(TAG, "CredentialManager unsupported on device. Falling back to GoogleSignIn Intent...")
            launchIntentChooser(activity, onLaunchIntentPicker, onError)
        } catch (e: GetCredentialCustomException) {
            Log.w(TAG, "GetCredentialCustomException (${e.type}): ${e.message}. Falling back to GoogleSignIn Intent...")
            launchIntentChooser(activity, onLaunchIntentPicker, onError)
        } catch (e: GetCredentialException) {
            Log.w(TAG, "GetCredentialException: ${e.message}. Falling back to GoogleSignIn Intent...")
            launchIntentChooser(activity, onLaunchIntentPicker, onError)
        } catch (e: Exception) {
            Log.w(TAG, "Unexpected error in CredentialManager: ${e.message}. Falling back to GoogleSignIn Intent...", e)
            launchIntentChooser(activity, onLaunchIntentPicker, onError)
        }
    }

    /**
     * Launches the native GoogleSignIn Intent account chooser dialog.
     */
    fun launchIntentChooser(
        activity: Activity,
        onLaunchIntentPicker: (Intent) -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            val client = getGoogleSignInClient(activity)
            // Sign out existing client state so the system account picker always prompts
            client.signOut().addOnCompleteListener {
                try {
                    val intent = client.signInIntent
                    Log.d(TAG, "Launching native GoogleSignIn Intent account chooser.")
                    onLaunchIntentPicker(intent)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to launch GoogleSignIn intent: ${e.message}", e)
                    onError("Unable to launch Google account picker: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error configuring GoogleSignIn Intent: ${e.message}", e)
            onError("Error initializing Google Sign-In: ${e.message}")
        }
    }

    /**
     * Handles the Activity Result from the native GoogleSignIn Intent.
     */
    fun handleGoogleSignInIntentResult(
        task: Task<GoogleSignInAccount>?,
        onSuccess: (GoogleAuthUser) -> Unit,
        onError: (String) -> Unit,
        onCancelled: () -> Unit
    ) {
        if (task == null) {
            Log.d(TAG, "GoogleSignIn Intent result task is null (User cancelled or no result).")
            onCancelled()
            return
        }

        try {
            val account = task.getResult(ApiException::class.java)
            if (account != null) {
                val idToken = account.idToken
                if (idToken.isNullOrBlank()) {
                    Log.e(TAG, "GoogleSignIn account returned with null/empty idToken.")
                    onError("Google authentication succeeded, but ID token is missing. Please verify Firebase Web Client ID configuration.")
                    return
                }

                val email = account.email ?: ""
                val displayName = account.displayName ?: if (email.contains("@")) email.substringBefore("@") else "MotionIQ Athlete"
                val photoUrl = account.photoUrl?.toString()
                val stableUserId = if (email.isNotBlank()) {
                    "google_" + kotlin.math.abs(email.lowercase().hashCode())
                } else {
                    "google_" + kotlin.math.abs(idToken.take(20).hashCode())
                }

                Log.d(TAG, "GoogleSignIn Intent succeeded! Email: $email, Name: $displayName, UID: $stableUserId")

                val user = GoogleAuthUser(
                    idToken = idToken,
                    userId = stableUserId,
                    email = email,
                    displayName = displayName,
                    photoUrl = photoUrl
                )
                onSuccess(user)
            } else {
                Log.w(TAG, "GoogleSignIn account was null.")
                onError("No Google account selected.")
            }
        } catch (e: ApiException) {
            val statusCode = e.statusCode
            Log.e(TAG, "GoogleSignIn Intent failed with ApiException status code: $statusCode (${GoogleSignInStatusCodes.getStatusCodeString(statusCode)})", e)

            when (statusCode) {
                GoogleSignInStatusCodes.SIGN_IN_CANCELLED,
                12501 -> {
                    Log.d(TAG, "User explicitly cancelled the Google account chooser dialog.")
                    onCancelled()
                }
                GoogleSignInStatusCodes.SIGN_IN_CURRENTLY_IN_PROGRESS,
                12502 -> {
                    Log.w(TAG, "Google Sign-In is already in progress.")
                    onError("Sign-in already in progress. Please check your screen.")
                }
                GoogleSignInStatusCodes.DEVELOPER_ERROR,
                10 -> {
                    val msg = "Google Sign-In configuration mismatch (Code 10: DEVELOPER_ERROR). Please ensure the SHA-1 signing certificate fingerprint and Package Name (com.aistudio.motioniq.fitapp) are registered in the Firebase / Google Cloud Console."
                    Log.e(TAG, msg)
                    onError(msg)
                }
                CommonStatusCodes.NETWORK_ERROR,
                7 -> {
                    onError("Network error during Google Sign-In. Please check your internet connection.")
                }
                CommonStatusCodes.SIGN_IN_REQUIRED,
                4 -> {
                    onError("Google Sign-In required. Please select or add a Google account.")
                }
                GoogleSignInStatusCodes.SIGN_IN_FAILED,
                12500 -> {
                    onError("Google Sign-In failed. Please update Google Play Services and verify device date/time.")
                }
                else -> {
                    val errorString = GoogleSignInStatusCodes.getStatusCodeString(statusCode)
                    onError("Google Sign-In failed ($errorString / Code $statusCode).")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected exception parsing GoogleSignIn Intent result: ${e.message}", e)
            onError(e.localizedMessage ?: "Unexpected error during Google Sign-In.")
        }
    }

    /**
     * Safely parses the JSON payload from a JWT token.
     */
    private fun parseJwtPayload(idToken: String): JSONObject? {
        return try {
            val parts = idToken.split(".")
            if (parts.size >= 2) {
                val decodedBytes = Base64.decode(
                    parts[1],
                    Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP
                )
                val jsonString = String(decodedBytes, Charsets.UTF_8)
                JSONObject(jsonString)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error decoding JWT payload: ${e.message}")
            null
        }
    }

    /**
     * Clears cached credentials and signs out from GoogleSignInClient and CredentialManager.
     */
    suspend fun signOut(context: Context) {
        val activity = findActivity(context) ?: (context as? Activity)
        Log.d(TAG, "Signing out from Google Sign-In providers...")

        // 1. Clear CredentialManager state
        try {
            val credentialManager = CredentialManager.create(context)
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
            Log.d(TAG, "CredentialManager state cleared.")
        } catch (e: Exception) {
            Log.w(TAG, "Error clearing CredentialManager state: ${e.message}")
        }

        // 2. Sign out GoogleSignInClient
        if (activity != null) {
            try {
                val client = getGoogleSignInClient(activity)
                client.signOut().addOnCompleteListener {
                    Log.d(TAG, "GoogleSignInClient sign out completed.")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error signing out GoogleSignInClient: ${e.message}")
            }
        }
    }
}
