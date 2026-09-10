package com.example.travira.screens.places

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.travira.auth.TokenManager
import com.example.travira.model.Place
import com.example.travira.remote.AddPlaceRequest
import com.example.travira.remote.CloudinaryUploader
import com.example.travira.remote.RetrofitInstance
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPlaceScreen(
    place: Place,
    tokenManager: TokenManager,
    onBack: () -> Unit,
    onSaved: (Place) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf(place.name) }
    var shortDescription by remember { mutableStateOf(place.shortDescription.orEmpty()) }
    var description by remember { mutableStateOf(place.description.orEmpty()) }
    var city by remember { mutableStateOf(place.city.orEmpty()) }
    var state by remember { mutableStateOf(place.state.orEmpty()) }
    var country by remember { mutableStateOf(place.country.orEmpty()) }
    var location by remember { mutableStateOf(place.location.orEmpty()) }
    var existingImageUrl by remember { mutableStateOf(place.imageUrl) }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var successMsg by remember { mutableStateOf<String?>(null) }

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) imageUri = uri
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit place") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF37474F),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFECEFF1))
                    .clickable { picker.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                when {
                    imageUri != null -> {
                        AsyncImage(
                            model = imageUri,
                            contentDescription = "New image",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    !existingImageUrl.isNullOrBlank() -> {
                        AsyncImage(
                            model = existingImageUrl,
                            contentDescription = "Current image",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.25f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Tap to change photo", color = Color.White, fontWeight = FontWeight.Medium)
                        }
                    }
                    else -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.AddAPhoto,
                                contentDescription = null,
                                tint = Color(0xFF546E7A),
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Tap to add photo", color = Color(0xFF546E7A))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            EditField("Place name *", name) { name = it }
            EditField("Short description", shortDescription) { shortDescription = it }
            EditField("Full description", description, singleLine = false) { description = it }
            EditField("City", city) { city = it }
            EditField("State", state) { state = it }
            EditField("Country", country) { country = it }
            EditField("Location (address or lat,lng)", location) { location = it }

            if (error != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(error!!, color = Color(0xFFC62828), fontSize = 13.sp)
            }
            if (successMsg != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(successMsg!!, color = Color(0xFF2E7D32), fontSize = 13.sp)
            }

            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = {
                    if (name.isBlank()) {
                        error = "Place name is required"
                        return@Button
                    }
                    scope.launch {
                        loading = true
                        error = null
                        successMsg = null
                        try {
                            val token = tokenManager.accessToken
                                ?: throw Exception("Not logged in")
                            var imageUrl = existingImageUrl
                            if (imageUri != null) {
                                imageUrl = CloudinaryUploader.uploadImage(context, imageUri!!)
                            }
                            val body = AddPlaceRequest(
                                name = name.trim(),
                                shortDescription = shortDescription.trim().ifBlank { null },
                                description = description.trim().ifBlank { null },
                                city = city.trim().ifBlank { null },
                                state = state.trim().ifBlank { null },
                                country = country.trim().ifBlank { null },
                                location = location.trim().ifBlank { null },
                                imageUrl = imageUrl
                            )
                            val res = RetrofitInstance.adminApi.updatePlace(
                                bearer = "Bearer $token",
                                id = place._id,
                                body = body
                            )
                            val updated = res.place ?: place.copy(
                                name = body.name,
                                shortDescription = body.shortDescription,
                                description = body.description,
                                city = body.city,
                                state = body.state,
                                country = body.country,
                                location = body.location,
                                imageUrl = body.imageUrl
                            )
                            successMsg = res.message ?: "Place saved"
                            kotlinx.coroutines.delay(700)
                            onSaved(updated)
                        } catch (e: Exception) {
                            error = e.message ?: "Failed to save changes"
                        } finally {
                            loading = false
                        }
                    }
                },
                enabled = !loading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF37474F))
            ) {
                if (loading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Save", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
private fun EditField(
    label: String,
    value: String,
    singleLine: Boolean = true,
    onChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp),
        shape = RoundedCornerShape(12.dp),
        singleLine = singleLine,
        minLines = if (singleLine) 1 else 3,
        maxLines = if (singleLine) 1 else 6
    )
}
