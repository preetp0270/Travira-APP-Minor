package com.example.travira.remote

import com.example.travira.model.NotificationItem
import com.example.travira.model.Place
import com.example.travira.model.User

// ── Places ──────────────────────────────────────────

data class PlacesResponse(
    val success: Boolean = false,
    val data: List<Place> = emptyList(),
    val message: String? = null
)

data class PlaceResponse(
    val success: Boolean = false,
    val place: Place? = null,
    val message: String? = null
)

data class AddPlaceRequest(
    val name: String,
    val shortDescription: String? = null,
    val description: String? = null,
    val city: String? = null,
    val state: String? = null,
    val country: String? = null,
    val location: String? = null,
    val imageUrl: String? = null
)

data class WishlistResponse(
    val success: Boolean = false,
    val wishlist: List<Place> = emptyList(),
    val message: String? = null
)

data class MyPlacesResponse(
    val success: Boolean = false,
    val places: List<Place> = emptyList(),
    val message: String? = null
)

data class VisitedPlacesResponse(
    val success: Boolean = false,
    val places: List<Place> = emptyList(),
    val message: String? = null
)

// ── Auth ────────────────────────────────────────────

data class LoginRequest(
    val email: String,
    val password: String
)

data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String
)

data class AuthUserDto(
    val id: String? = null,
    val _id: String? = null,
    val name: String = "",
    val email: String = "",
    val role: String = "user",
    val phone: String? = null,
    val location: String? = null
)

data class LoginResponse(
    val message: String? = null,
    val accessToken: String = "",
    val refreshToken: String = "",
    val user: AuthUserDto? = null
)

data class RegisterResponse(
    val message: String? = null,
    val user: AuthUserDto? = null
)

data class RefreshRequest(
    val refreshToken: String
)

data class RefreshResponse(
    val accessToken: String = ""
)

data class UserProfileResponse(
    val success: Boolean = false,
    val user: User? = null,
    val message: String? = null
)

data class UpdateProfileRequest(
    val name: String? = null,
    val phone: String? = null,
    val location: String? = null,
    val bio: String? = null
)

data class MarkNotificationsReadRequest(
    val ids: List<String>? = null
)

data class AdminUpdateUserRequest(
    val name: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val location: String? = null,
    val bio: String? = null,
    val password: String? = null,
    val role: String? = null
)

data class SimpleMessageResponse(
    val success: Boolean = false,
    val message: String? = null,
    val visitorsCount: Int? = null,
    val averageRating: Double? = null,
    val ratingsCount: Int? = null
)

data class RatePlaceRequest(
    val value: Int,
    val feedback: String? = null
)

data class RatePlaceResponse(
    val success: Boolean = false,
    val message: String? = null,
    val averageRating: Double = 0.0,
    val ratingsCount: Int = 0,
    val visitorsCount: Int = 0
)

data class NotificationsResponse(
    val success: Boolean = false,
    val notifications: List<NotificationItem> = emptyList(),
    val message: String? = null
)

// ── Admin DTOs ──────────────────────────────────────

data class AdminPlacesResponse(
    val success: Boolean = false,
    val places: List<Place> = emptyList(),
    val counts: PlaceCounts? = null
)

data class PlaceCounts(
    val total: Int = 0
)

data class AdminPlaceDetailResponse(
    val success: Boolean = false,
    val place: Place? = null,
    val stats: PlaceStats? = null
)

data class PlaceStats(
    val visitorsCount: Int = 0,
    val averageRating: Double = 0.0,
    val ratingsCount: Int = 0,
    val wishlistCount: Int = 0
)

data class AdminUsersResponse(
    val success: Boolean = false,
    val users: List<User> = emptyList()
)

data class AdminUserDetailResponse(
    val success: Boolean = false,
    val user: User? = null,
    val passwordNote: String? = null,
    val message: String? = null
)
