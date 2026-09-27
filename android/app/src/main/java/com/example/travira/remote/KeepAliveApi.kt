package com.example.travira.remote

import retrofit2.http.GET

// Lightweight health endpoints used by ServerKeepAlive
// to prevent Render free-tier cold starts while the app is open.

data class HealthResponse(
    val success: Boolean = false,
    val status: String? = null,
    val pong: Boolean? = null,
    val ts: Long? = null,
    val uptime: Double? = null
)

interface KeepAliveApi {
    @GET("api/ping")
    suspend fun ping(): HealthResponse

    @GET("api/health")
    suspend fun health(): HealthResponse
}
