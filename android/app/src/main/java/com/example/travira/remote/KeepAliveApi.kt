package com.example.travira.remote


/**
 * Lightweight health endpoints used by ServerKeepAlive
 * to prevent Render free-tier cold starts while the app is open.
 */

import retrofit2.http.GET

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
