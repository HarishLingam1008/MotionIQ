package com.example.util

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import org.json.JSONObject
import java.io.InputStream
import java.security.MessageDigest

data class FingerprintComparisonResult(
    val currentBuildSha1: String,
    val currentBuildSha256: String,
    val packageName: String,
    val registeredFirebaseSha1s: List<String>,
    val isMatchedInFirebaseConfig: Boolean,
    val statusSummary: String,
    val alertDetails: String
)

object FingerprintVerificationUtil {

    /**
     * Retrieves the current build variant's signing certificate SHA-1 or SHA-256 fingerprint.
     */
    fun getAppCertificateFingerprint(context: Context, algorithm: String = "SHA-1"): String {
        return try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_SIGNING_CERTIFICATES
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_SIGNATURES
                )
            }

            val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.signingInfo?.apkContentsSigners
            } else {
                @Suppress("DEPRECATION")
                packageInfo.signatures
            }

            val cert = signatures?.firstOrNull()?.toByteArray() ?: return "Unavailable"
            val md = MessageDigest.getInstance(algorithm)
            val publicKey = md.digest(cert)

            publicKey.joinToString(":") { byte ->
                "%02X".format(byte)
            }
        } catch (e: Exception) {
            "Error: ${e.localizedMessage ?: e.message}"
        }
    }

    /**
     * Retrieves the current SHA-1 and compares it against Firebase Console registered fingerprints.
     */
    fun compareSha1WithFirebaseConfig(context: Context): FingerprintComparisonResult {
        val currentSha1 = getAppCertificateFingerprint(context, "SHA-1")
        val currentSha256 = getAppCertificateFingerprint(context, "SHA-256")
        val packageName = context.packageName

        val registeredSha1s = mutableListOf<String>()

        try {
            val inputStream: InputStream? = try {
                context.assets.open("google-services.json")
            } catch (e: Exception) {
                val resId = context.resources.getIdentifier("google_services", "raw", packageName)
                if (resId != 0) context.resources.openRawResource(resId) else null
            }

            if (inputStream != null) {
                val jsonString = inputStream.bufferedReader().use { it.readText() }
                val root = JSONObject(jsonString)
                val clients = root.optJSONArray("client")
                if (clients != null) {
                    for (i in 0 until clients.length()) {
                        val client = clients.getJSONObject(i)
                        val oauthClients = client.optJSONArray("oauth_client")
                        if (oauthClients != null) {
                            for (j in 0 until oauthClients.length()) {
                                val oauth = oauthClients.getJSONObject(j)
                                val certHash = oauth.optString("certificate_hash", "")
                                if (certHash.isNotEmpty()) {
                                    val formattedHash = formatFingerprint(certHash)
                                    registeredSha1s.add(formattedHash)
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Asset reading is fallback
        }

        val normalizedCurrent = currentSha1.replace(":", "").uppercase()
        val isMatch = registeredSha1s.any { registered ->
            registered.replace(":", "").uppercase() == normalizedCurrent
        }

        val statusSummary: String
        val alertDetails: String

        if (registeredSha1s.isEmpty()) {
            statusSummary = "REQUIRES VERIFICATION IN FIREBASE CONSOLE"
            alertDetails = "Current Build Variant SHA-1:\n$currentSha1\n\n" +
                    "Package Name: $packageName\n\n" +
                    "⚠️ ATTENTION REQUIRED: Verify that this exact SHA-1 fingerprint ($currentSha1) is added under Firebase Console > Project Settings > Android Apps ($packageName) > SHA certificate fingerprints."
        } else if (isMatch) {
            statusSummary = "FINGERPRINT MATCH CONFIRMED!"
            alertDetails = "Success: Current build SHA-1 fingerprint ($currentSha1) MATCHES the fingerprint registered in the Firebase configuration."
        } else {
            statusSummary = "FINGERPRINT MISMATCH DETECTED!"
            alertDetails = "CRITICAL MISMATCH: The current build variant SHA-1 ($currentSha1) DOES NOT MATCH the fingerprints registered in Firebase configuration (${registeredSha1s.joinToString(", ")}).\n\n" +
                    "Alert: Update Firebase Console with SHA-1: $currentSha1 to resolve Google Sign-In failures."
        }

        return FingerprintComparisonResult(
            currentBuildSha1 = currentSha1,
            currentBuildSha256 = currentSha256,
            packageName = packageName,
            registeredFirebaseSha1s = registeredSha1s,
            isMatchedInFirebaseConfig = isMatch,
            statusSummary = statusSummary,
            alertDetails = alertDetails
        )
    }

    private fun formatFingerprint(rawHex: String): String {
        val clean = rawHex.replace(":", "").uppercase()
        return clean.chunked(2).joinToString(":")
    }
}
