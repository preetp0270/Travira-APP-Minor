package com.example.travira.screens.places

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.filled.Link
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
import com.example.travira.remote.AddPlaceRequest
import com.example.travira.remote.CloudinaryUploader
import com.example.travira.remote.RetrofitInstance
import kotlinx.coroutines.launch

internal enum class ImageInputMode { FILE, URL }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPlaceScreen(
    tokenManager: TokenManager,
    onBack: () -> Unit,
    onSubmitted: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf("") }
    var shortDescription by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var state by remember { mutableStateOf("") }
    var country by remember { mutableStateOf("India") }
    var location by remember { mutableStateOf("") }
    var imageMode by remember { mutableStateOf(ImageInputMode.FILE) }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var imageLink by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var successMsg by remember { mutableStateOf<String?>(null) }

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            imageUri = uri
            imageLink = ""
        }
    }

    val previewModel: Any? = when {
        imageMode == ImageInputMode.FILE && imageUri != null -> imageUri
        imageMode == ImageInputMode.URL && imageLink.isNotBlank() -> imageLink.trim()
        else -> null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admin · Add Place") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1565C0),
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
            Text(
                "Place image",
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = Color(0xFF37474F)
            )
            Spacer(modifier = Modifier.height(8.dp))
            ImageModeToggle(
                mode = imageMode,
                onModeChange = { imageMode = it },
                accent = Color(0xFF1565C0)
            )
            Spacer(modifier = Modifier.height(10.dp))

            if (imageMode == ImageInputMode.FILE) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFE3F2FD))
                        .clickable { picker.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    if (imageUri != null) {
                        AsyncImage(
                            model = imageUri,
                            contentDescription = "Selected image",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.AddAPhoto,
                                contentDescription = null,
                                tint = Color(0xFF1565C0),
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Tap to pick photo (JPG, PNG…)", color = Color(0xFF1565C0))
                        }
                    }
                }
                Text(
                    "File is uploaded to Cloudinary when you publish, then the URL is saved in MongoDB.",
                    fontSize = 12.sp,
                    color = Color(0xFF78909C),
                    modifier = Modifier.padding(top = 6.dp)
                )
            } else {
                OutlinedTextField(
                    value = imageLink,
                    onValueChange = {
                        imageLink = it
                        if (it.isNotBlank()) imageUri = null
                    },
                    label = { Text("Image URL from anywhere") },
                    placeholder = { Text("https://example.com/photo.jpg") },
                    leadingIcon = {
                        Icon(Icons.Default.Link, contentDescription = null, tint = Color(0xFF1565C0))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                Text(
                    "Any public image link. On publish we fetch it into Cloudinary, then store the Cloudinary URL in MongoDB.",
                    fontSize = 12.sp,
                    color = Color(0xFF78909C),
                    modifier = Modifier.padding(top = 6.dp)
                )
                if (previewModel != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    AsyncImage(
                        model = previewModel,
                        contentDescription = "URL preview",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .clip(RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Field("Place name *", name) { name = it }
            Field("Short description", shortDescription) { shortDescription = it }
            Field("Full description", description) { description = it }
            Field("City", city) { city = it }
            Field("State", state) { state = it }
            Field("Country", country) { country = it }
            Field("Location (lat, lng or address)", location) { location = it }

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
                        error = "Name is required"
                        return@Button
                    }
                    val token = tokenManager.accessToken
                    if (token.isNullOrBlank()) {
                        error = "Please login first"
                        return@Button
                    }
                    loading = true
                    error = null
                    successMsg = null
                    scope.launch {
                        try {
                            var imageUrl: String? = null
                            when (imageMode) {
                                ImageInputMode.FILE -> {
                                    if (imageUri != null) {
                                        imageUrl = CloudinaryUploader.uploadImage(context, imageUri!!)
                                    }
                                }
                                ImageInputMode.URL -> {
                                    val link = imageLink.trim()
                                    if (link.isNotBlank()) {
                                        if (CloudinaryUploader.isCloudinaryUrl(link)) {
                                            imageUrl = link
                                        } else {
                                            val imp = RetrofitInstance.adminApi.importImage(
                                                bearer = "Bearer $token",
                                                body = com.example.travira.remote.ImportImageRequest(link)
                                            )
                                            imageUrl = imp.imageUrl
                                                ?: throw Exception(imp.message ?: "Image import failed")
                                        }
                                    }
                                }
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
                            val res = RetrofitInstance.adminApi.addPlace(
                                bearer = "Bearer $token",
                                body = body
                            )
                            successMsg = res.message ?: "Place published"
                            onSubmitted()
                        } catch (e: Exception) {
                            error = e.message ?: "Failed to submit place"
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
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0))
            ) {
                if (loading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Text(
                        "Publish place",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Your place goes live on the home feed immediately after publish.",
                fontSize = 12.sp,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
internal fun ImageModeToggle(
    mode: ImageInputMode,
    onModeChange: (ImageInputMode) -> Unit,
    accent: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ModeChip(
            label = "Upload file",
            selected = mode == ImageInputMode.FILE,
            accent = accent,
            modifier = Modifier.weight(1f),
            onClick = { onModeChange(ImageInputMode.FILE) }
        )
        ModeChip(
            label = "Image link",
            selected = mode == ImageInputMode.URL,
            accent = accent,
            modifier = Modifier.weight(1f),
            onClick = { onModeChange(ImageInputMode.URL) }
        )
    }
}

@Composable
private fun ModeChip(
    label: String,
    selected: Boolean,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) accent else Color(0xFFECEFF1))
            .border(
                width = 1.dp,
                color = if (selected) accent else Color(0xFFB0BEC5),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = if (selected) Color.White else Color(0xFF37474F),
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun Field(
    label: String,
    value: String,
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
        singleLine = label != "Full description"
    )
}
