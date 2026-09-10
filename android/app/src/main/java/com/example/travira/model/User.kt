package com.example.travira.model

data class User(
    val id: String = "",
    val _id: String = "",
    val name: String = "",
    val email: String = "",
    val role: String = "user",
    val phone: String? = null,
    val location: String? = null,
    val bio: String? = null,
    val wishlist: List<Place> = emptyList(),
    val addedPlaces: List<Place> = emptyList(),
    val visitedPlaces: List<VisitedPlaceEntry> = emptyList(),
    val notifications: List<NotificationItem> = emptyList()
) {
    val userId: String get() = id.ifBlank { _id }
}

data class VisitedPlaceEntry(
    val place: Place? = null,
    val visitedAt: String? = null
)

data class NotificationItem(
    val _id: String? = null,
    val title: String = "",
    val message: String = "",
    val read: Boolean = false,
    val createdAt: String? = null
)
