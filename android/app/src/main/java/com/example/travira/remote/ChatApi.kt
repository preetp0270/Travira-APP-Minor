package com.example.travira.remote

import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

// Chat API: POST api/chat (requires Bearer token).
// Backend uses Gemini 3.8 Flash with 3.5 Flash-Lite fallback.

data class ChatHistoryTurn(
    val role: String, // "user" | "model"
    val text: String
)

data class ChatRequest(
    val message: String,
    val history: List<ChatHistoryTurn> = emptyList()
)

data class ChatResponse(
    val success: Boolean = false,
    val reply: String? = null,
    val message: String? = null,
    val model: String? = null
)

interface ChatApi {
    @POST("api/chat")
    suspend fun chat(
        @Header("Authorization") bearer: String,
        @Body body: ChatRequest
    ): ChatResponse
}
