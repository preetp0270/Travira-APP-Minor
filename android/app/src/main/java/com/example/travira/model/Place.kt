package com.example.travira.model

/**
 * Matches the live API response from GET /api/places
 * (raw JSON with optional populated addedBy).
 */
data class Place(
    val _id: String = "",
    val id: Int = 0,
    val name: String = "",
    val shortDescription: String? = null,
    val description: String? = null,
    val city: String? = null,
    val state: String? = null,
    val country: String? = null,
    val location: String? = null,
    val imageUrl: String? = null,
    val rating: Double = 0.0,
    val averageRating: Double = 0.0,
    val visitorsCount: Int = 0,
    val ratingsCount: Int = 0,
    val createdAt: String? = null,
    /** Populated by backend as { _id, name, email } or raw ObjectId string */
    val addedBy: AddedByUser? = null
) {
    /** Prefer live `rating`, fall back to averageRating */
    val displayRating: Double
        get() = if (rating > 0) rating else averageRating

    val locationLine: String
        get() = listOfNotNull(city, state, country)
            .filter { it.isNotBlank() }
            .joinToString(", ")
            .ifBlank { location.orEmpty() }

    val countryOrFallback: String
        get() = country?.takeIf { it.isNotBlank() }
            ?: state?.takeIf { it.isNotBlank() }
            ?: "—"

    val addedByName: String
        get() = addedBy?.name?.takeIf { it.isNotBlank() } ?: "Traveler"

    val addedById: String
        get() = addedBy?.idOrEmpty().orEmpty()
}

data class AddedByUser(
    val _id: String? = null,
    val id: String? = null,
    val name: String? = null,
    val email: String? = null
) {
    fun idOrEmpty(): String = (_id ?: id).orEmpty()
}
