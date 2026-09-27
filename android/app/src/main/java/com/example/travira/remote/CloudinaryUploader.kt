package com.example.travira.remote

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

/**
 * Uploads an image to Cloudinary (unsigned preset) and returns the secure URL.
 * Supports local files (Uri) and remote image URLs (Cloudinary fetches them).
 * Local images are downscaled + JPEG-compressed before upload.
 */
object CloudinaryUploader {

    private const val CLOUD_NAME = "yv3rd7a3"
    private const val UPLOAD_PRESET = "travira_unsigned"
    private const val MAX_SIDE = 1280
    private const val JPEG_QUALITY = 82

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(45, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    fun isCloudinaryUrl(url: String?): Boolean =
        !url.isNullOrBlank() &&
            url.startsWith("https://res.cloudinary.com/", ignoreCase = true)

    suspend fun uploadImage(context: Context, imageUri: Uri): String = withContext(Dispatchers.IO) {
        val bytes = compressImage(context, imageUri)
            ?: throw Exception("Could not read or compress image. Try a smaller photo.")

        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("upload_preset", UPLOAD_PRESET)
            .addFormDataPart("folder", "travira")
            .addFormDataPart(
                "file",
                "travira_${System.currentTimeMillis()}.jpg",
                bytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
            )
            .build()

        postUpload(body)
    }

    /**
     * Fetch a public image URL into Cloudinary and return the new secure_url.
     * If [imageUrl] is already a Cloudinary URL, returns it unchanged.
     */
    suspend fun uploadImageFromUrl(imageUrl: String): String = withContext(Dispatchers.IO) {
        val trimmed = imageUrl.trim()
        if (trimmed.isBlank()) {
            throw Exception("Image URL is empty")
        }
        if (isCloudinaryUrl(trimmed)) {
            return@withContext trimmed
        }
        if (!trimmed.startsWith("http://", ignoreCase = true) &&
            !trimmed.startsWith("https://", ignoreCase = true)
        ) {
            throw Exception("Image URL must start with http:// or https://")
        }

        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("upload_preset", UPLOAD_PRESET)
            .addFormDataPart("folder", "travira")
            // Cloudinary fetches the remote URL server-side
            .addFormDataPart("file", trimmed)
            .build()

        postUpload(body)
    }

    private fun postUpload(body: MultipartBody): String {
        val request = Request.Builder()
            .url("https://api.cloudinary.com/v1_1/$CLOUD_NAME/image/upload")
            .post(body)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val json = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    val msg = try {
                        JSONObject(json).optString("error").ifBlank {
                            JSONObject(json).optJSONObject("error")?.optString("message")
                        } ?: json.take(180)
                    } catch (_: Exception) {
                        json.take(180).ifBlank { "HTTP ${response.code}" }
                    }
                    throw Exception("Image upload failed: $msg")
                }
                val secureUrl = JSONObject(json).optString("secure_url")
                if (secureUrl.isBlank()) {
                    throw Exception("Upload succeeded but no image URL returned")
                }
                return secureUrl
            }
        } catch (e: Exception) {
            if (e.message?.startsWith("Image upload") == true ||
                e.message?.startsWith("Upload succeeded") == true ||
                e.message?.startsWith("Could not") == true ||
                e.message?.startsWith("Image URL") == true
            ) {
                throw e
            }
            throw Exception("Image upload failed: ${e.message ?: "network error"}")
        }
    }

    /** Decode, scale down if needed, encode as JPEG. */
    private fun compressImage(context: Context, uri: Uri): ByteArray? {
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, bounds)
            }

            var sample = 1
            val maxDim = maxOf(bounds.outWidth, bounds.outHeight).coerceAtLeast(1)
            while (maxDim / sample > MAX_SIDE * 2) {
                sample *= 2
            }

            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            val bitmap = context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, opts)
            } ?: return null

            val scaled = if (bitmap.width > MAX_SIDE || bitmap.height > MAX_SIDE) {
                val ratio = minOf(
                    MAX_SIDE.toFloat() / bitmap.width,
                    MAX_SIDE.toFloat() / bitmap.height
                )
                val w = (bitmap.width * ratio).toInt().coerceAtLeast(1)
                val h = (bitmap.height * ratio).toInt().coerceAtLeast(1)
                Bitmap.createScaledBitmap(bitmap, w, h, true).also {
                    if (it !== bitmap) bitmap.recycle()
                }
            } else {
                bitmap
            }

            val out = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
            if (scaled !== bitmap) scaled.recycle() else bitmap.recycle()
            out.toByteArray()
        } catch (_: Exception) {
            // Fallback: raw bytes (may still work for small files)
            try {
                context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            } catch (_: Exception) {
                null
            }
        }
    }
}
