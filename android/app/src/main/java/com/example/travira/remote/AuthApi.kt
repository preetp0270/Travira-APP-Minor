package com.example.travira.remote

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface AuthApi {

    @POST("api/users/register")
    suspend fun register(@Body body: RegisterRequest): RegisterResponse

    @POST("api/users/login")
    suspend fun login(@Body body: LoginRequest): LoginResponse

    @POST("api/users/refresh-token")
    suspend fun refreshToken(@Body body: RefreshRequest): RefreshResponse

    @GET("api/users/me")
    suspend fun getCurrentUser(
        @Header("Authorization") bearer: String
    ): UserProfileResponse

    @GET("api/users/profile")
    suspend fun getProfile(
        @Header("Authorization") bearer: String
    ): UserProfileResponse

    @PUT("api/users/profile")
    suspend fun updateProfile(
        @Header("Authorization") bearer: String,
        @Body body: UpdateProfileRequest
    ): UserProfileResponse

    @GET("api/users/notifications")
    suspend fun getNotifications(
        @Header("Authorization") bearer: String
    ): NotificationsResponse

    @PUT("api/users/notifications/read")
    suspend fun markNotificationsRead(
        @Header("Authorization") bearer: String,
        @Body body: MarkNotificationsReadRequest = MarkNotificationsReadRequest()
    ): NotificationsResponse

    @GET("api/users/visited")
    suspend fun getVisitedPlaces(
        @Header("Authorization") bearer: String
    ): VisitedPlacesResponse

    @POST("api/users/visited/{id}")
    suspend fun addVisitedPlace(
        @Header("Authorization") bearer: String,
        @Path("id") id: String
    ): SimpleMessageResponse

    @DELETE("api/users/visited/{id}")
    suspend fun removeVisitedPlace(
        @Header("Authorization") bearer: String,
        @Path("id") id: String
    ): SimpleMessageResponse

    @POST("api/users/logout")
    suspend fun logout(
        @Header("Authorization") bearer: String,
        @Body body: RefreshRequest
    ): SimpleMessageResponse
}
