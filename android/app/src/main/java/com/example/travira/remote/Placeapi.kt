package com.example.travira.remote

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

interface PlaceApi {

    @GET("api/place")
    suspend fun getPlaces(): PlacesResponse

    @GET("api/place/{id}")
    suspend fun getPlaceById(@Path("id") id: String): PlaceResponse

    @GET("api/place/user/my-places")
    suspend fun getMyPlaces(
        @Header("Authorization") bearer: String
    ): MyPlacesResponse

    @POST("api/place/{id}/wishlist")
    suspend fun addWishlist(
        @Header("Authorization") bearer: String,
        @Path("id") id: String
    ): SimpleMessageResponse

    @DELETE("api/place/{id}/wishlist")
    suspend fun removeWishlist(
        @Header("Authorization") bearer: String,
        @Path("id") id: String
    ): SimpleMessageResponse

    @GET("api/place/user/wishlist")
    suspend fun getWishlist(
        @Header("Authorization") bearer: String
    ): WishlistResponse

    @POST("api/place/{id}/rating")
    suspend fun ratePlace(
        @Header("Authorization") bearer: String,
        @Path("id") id: String,
        @Body body: RatePlaceRequest
    ): RatePlaceResponse
}
