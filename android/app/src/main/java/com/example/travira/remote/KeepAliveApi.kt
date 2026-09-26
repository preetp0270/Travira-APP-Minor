package com.example.travira.remote

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
