package com.example.travira.remote

import com.example.travira.model.AddedByUser
import com.example.travira.model.Place
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.lang.reflect.Type
import java.util.concurrent.TimeUnit

/**
 * Accepts either a populated user object or a raw ObjectId string for addedBy.
 * Parses the object manually — never calls context.deserialize(AddedByUser)
 * (that would recurse and crash the app).
 */
private class AddedByUserDeserializer : JsonDeserializer<AddedByUser?> {
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): AddedByUser? {
        if (json == null || json.isJsonNull) return null
        return try {
            when {
                json.isJsonPrimitive -> {
                    val id = json.asString
                    if (id.isBlank()) null else AddedByUser(_id = id)
                }
                json.isJsonObject -> {
                    val o: JsonObject = json.asJsonObject
                    fun str(key: String): String? =
                        o.get(key)?.takeIf { it.isJsonPrimitive }?.asString
                    AddedByUser(
                        _id = str("_id"),
                        id = str("id"),
                        name = str("name"),
                        email = str("email")
                    )
                }
                else -> null
            }
        } catch (_: Exception) {
            null
        }
    }
}

/**
 * Accepts an array of place objects OR raw ObjectId strings (unpopulated refs).
 */
private class PlaceListDeserializer : JsonDeserializer<List<Place>> {
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): List<Place> {
        if (json == null || !json.isJsonArray) return emptyList()
        val out = ArrayList<Place>()
        for (el in json.asJsonArray) {
            try {
                when {
                    el == null || el.isJsonNull -> continue
                    el.isJsonPrimitive -> {
                        val id = el.asString
                        if (id.isNotBlank()) out.add(Place(_id = id))
                    }
                    el.isJsonObject -> {
                        val o = el.asJsonObject
                        fun str(key: String): String? =
                            o.get(key)?.takeIf { !it.isJsonNull && it.isJsonPrimitive }?.asString
                        fun dbl(key: String): Double =
                            try {
                                o.get(key)?.takeIf { it.isJsonPrimitive }?.asDouble ?: 0.0
                            } catch (_: Exception) {
                                0.0
                            }
                        fun int(key: String): Int =
                            try {
                                o.get(key)?.takeIf { it.isJsonPrimitive }?.asInt ?: 0
                            } catch (_: Exception) {
                                0
                            }
                        out.add(
                            Place(
                                _id = str("_id").orEmpty(),
                                name = str("name").orEmpty(),
                                shortDescription = str("shortDescription"),
                                description = str("description"),
                                city = str("city"),
                                state = str("state"),
                                country = str("country"),
                                location = str("location"),
                                imageUrl = str("imageUrl"),
                                rating = dbl("rating"),
                                averageRating = dbl("averageRating"),
                                visitorsCount = int("visitorsCount"),
                                ratingsCount = int("ratingsCount"),
                                approvalStatus = str("approvalStatus"),
                                adminFeedback = str("adminFeedback"),
                                createdAt = str("createdAt"),
                                addedBy = null
                            )
                        )
                    }
                }
            } catch (_: Exception) {
                // skip bad element
            }
        }
        return out
    }
}

object RetrofitInstance {

    const val BASE_URL = "https://travira-app-minor.onrender.com/"

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            // Render free tier can cold-start for 30–50s; keep generous timeouts
            .connectTimeout(45, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .writeTimeout(90, TimeUnit.SECONDS)
            .callTimeout(120, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()
    }

    private val gson by lazy {
        val placeListType = object : TypeToken<List<Place>>() {}.type
        GsonBuilder()
            .registerTypeAdapter(AddedByUser::class.java, AddedByUserDeserializer())
            .registerTypeAdapter(placeListType, PlaceListDeserializer())
            .create()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    val placeApi: PlaceApi by lazy { retrofit.create(PlaceApi::class.java) }
    val authApi: AuthApi by lazy { retrofit.create(AuthApi::class.java) }
    val adminApi: AdminApi by lazy { retrofit.create(AdminApi::class.java) }
    val chatApi: ChatApi by lazy { retrofit.create(ChatApi::class.java) }

    /** @deprecated use placeApi */
    val api: PlaceApi get() = placeApi
}
