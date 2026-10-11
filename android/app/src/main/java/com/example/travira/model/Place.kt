package com.example.travira.model

/**
 * Matches the live API response from GET /api/places.
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
    val ratingsCount: Int = 0
) {
    /**
     * Prefer averageRating from real reviews when present;
     * otherwise fall back to rating field.
     */
    val displayRating: Double
        get() = when {
            averageRating > 0 -> averageRating
            rating > 0 -> rating
            else -> 0.0
        }

    val locationLine: String
        get() = listOfNotNull(city, state, country)
            .filter { it.isNotBlank() }
            .joinToString(", ")
            .ifBlank { location.orEmpty() }

    val countryOrFallback: String
        get() = country?.takeIf { it.isNotBlank() }
            ?: state?.takeIf { it.isNotBlank() }
            ?: "—"
}
