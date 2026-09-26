package com.example.travira.remote

import org.json.JSONObject
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Turns Retrofit / network failures into short, user-friendly messages
 * instead of raw "HTTP 404" or stack-looking text.
 */
object ApiErrorHelper {

    fun message(throwable: Throwable?): String {
        if (throwable == null) return "Something went wrong. Please try again."
        return when (throwable) {
            is HttpException -> httpMessage(throwable)
            is UnknownHostException ->
                "No internet connection. Check your network and try again."
            is SocketTimeoutException ->
                "Server is taking too long (cold start?). Pull to refresh in a few seconds."
            is IOException ->
                "Network error. Please check your connection."
            else -> {
                val m = throwable.message.orEmpty()
                when {
                    m.contains("404", ignoreCase = true) ->
                        "Service temporarily unavailable (404). The server may be waking up — wait a moment and retry."
                    m.contains("401", ignoreCase = true) ||
                        m.contains("Unauthorized", ignoreCase = true) ->
                        "Session expired. Please log in again."
                    m.contains("503", ignoreCase = true) ->
                        "Travel chatbot is not configured on the server yet (missing GEMINI_API_KEY)."
                    m.isNotBlank() -> m
                    else -> "Something went wrong. Please try again."
                }
            }
        }
    }

    private fun httpMessage(e: HttpException): String {
        val code = e.code()
        val bodyMsg = try {
            val raw = e.response()?.errorBody()?.string().orEmpty()
            if (raw.isBlank()) null
            else {
                val json = JSONObject(raw)
                json.optString("message").takeIf { it.isNotBlank() }
                    ?: json.optString("error").takeIf { it.isNotBlank() }
            }
        } catch (_: Exception) {
            null
        }

        return when (code) {
            401 -> bodyMsg ?: "Please log in again (session expired)."
            403 -> bodyMsg ?: "You don’t have permission for this action."
            404 -> bodyMsg
                ?: "API route not found (404). Server may be restarting — retry shortly."
            502, 503 -> bodyMsg
                ?: "Server or AI service is unavailable. Check GEMINI_API_KEY on Render and retry."
            500 -> bodyMsg ?: "Server error. Please try again later."
            else -> bodyMsg ?: "Request failed (HTTP $code)."
        }
    }
}
