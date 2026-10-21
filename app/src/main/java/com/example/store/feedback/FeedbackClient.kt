package com.example.store.feedback

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

sealed class FeedbackResult {
    object Success : FeedbackResult()
    object NoInternet : FeedbackResult()
    data class Error(val message: String) : FeedbackResult()
}

/**
 * Handles feedback submission to the internal support destination.
 *
 * CRITICAL PRIVACY RULE:
 * The support email address is strictly private and internal.
 * It is never shown in the UI, toasts, dialogs, or string resources.
 */
object FeedbackClient {

    private const val TAG = "FeedbackClient"

    // Internal support recipient (never exposed to UI)
    private const val SUPPORT_DESTINATION = "meatmate.support@gmail.com"

    // Dedicated backend endpoint for feedback routing.
    // Configure this URL to your deployed serverless function or webhook service.
    private const val FEEDBACK_ENDPOINT = "https://formspree.io/f/meatmate_feedback"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    fun isOnline(context: Context): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val network = cm.activeNetwork ?: return false
                val capabilities = cm.getNetworkCapabilities(network) ?: return false
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            } else {
                @Suppress("DEPRECATION")
                val networkInfo = cm.activeNetworkInfo ?: return false
                @Suppress("DEPRECATION")
                networkInfo.isConnected
            }
        } catch (e: Exception) {
            true
        }
    }

    suspend fun submitFeedback(context: Context, feedbackMessage: String): FeedbackResult = withContext(Dispatchers.IO) {
        val trimmed = feedbackMessage.trim()
        if (trimmed.isEmpty()) {
            return@withContext FeedbackResult.Error("Message cannot be empty")
        }

        if (!isOnline(context)) {
            return@withContext FeedbackResult.NoInternet
        }

        try {
            val payload = JSONObject().apply {
                put("recipient", SUPPORT_DESTINATION)
                put("message", trimmed)
                put("timestamp", System.currentTimeMillis())
                put("osVersion", "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
                put("device", "${Build.MANUFACTURER} ${Build.MODEL}")
            }

            val requestBody = payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url(FEEDBACK_ENDPOINT)
                .post(requestBody)
                .header("Accept", "application/json")
                .header("User-Agent", "MeatMate-Android/${Build.VERSION.SDK_INT}")
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Log.i(TAG, "Feedback submitted successfully: code ${response.code}")
                    FeedbackResult.Success
                } else {
                    Log.w(TAG, "Feedback submission rejected with HTTP ${response.code}")
                    FeedbackResult.Error("Server returned code ${response.code}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Feedback network request failed", e)
            FeedbackResult.Error(e.localizedMessage ?: "Network connection error")
        }
    }
}
