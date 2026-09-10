package com.example.travira.remote

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface AdminApi {

    @GET("api/admin/places")
    suspend fun getAllPlaces(
        @Header("Authorization") bearer: String,
        @Query("status") status: String? = "all"
    ): AdminPlacesResponse

    @GET("api/admin/places/{id}")
    suspend fun getPlaceDetail(
        @Header("Authorization") bearer: String,
        @Path("id") id: String
    ): AdminPlaceDetailResponse

    @DELETE("api/admin/places/{id}")
    suspend fun deletePlace(
        @Header("Authorization") bearer: String,
        @Path("id") id: String
    ): SimpleMessageResponse

    @PUT("api/admin/places/{id}")
    suspend fun updatePlace(
        @Header("Authorization") bearer: String,
        @Path("id") id: String,
        @Body body: AddPlaceRequest
    ): PlaceResponse

    @POST("api/admin/places")
    suspend fun addPlace(
        @Header("Authorization") bearer: String,
        @Body body: AddPlaceRequest
    ): PlaceResponse

    @GET("api/admin/users")
    suspend fun getUsers(
        @Header("Authorization") bearer: String
    ): AdminUsersResponse

    @GET("api/admin/users/{id}")
    suspend fun getUserDetail(
        @Header("Authorization") bearer: String,
        @Path("id") id: String
    ): AdminUserDetailResponse

    @POST("api/admin/users")
    suspend fun createUser(
        @Header("Authorization") bearer: String,
        @Body body: Map<String, String>
    ): SimpleMessageResponse

    @PUT("api/admin/users/{id}")
    suspend fun updateUser(
        @Header("Authorization") bearer: String,
        @Path("id") id: String,
        @Body body: AdminUpdateUserRequest
    ): AdminUserDetailResponse

    @DELETE("api/admin/users/{id}")
    suspend fun deleteUser(
        @Header("Authorization") bearer: String,
        @Path("id") id: String
    ): SimpleMessageResponse
}
