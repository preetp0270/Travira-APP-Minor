package com.example.travira.screens.places

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.travira.auth.TokenManager
import com.example.travira.model.Place
import com.example.travira.remote.RatePlaceRequest
import com.example.travira.remote.RetrofitInstance
import kotlinx.coroutines.launch

@Composable
fun PlaceScreen(
    place: Place,
    onBackClick: () -> Unit,
    tokenManager: TokenManager? = null,
    isWishlistedInitially: Boolean = false,
    isVisitedInitially: Boolean = false,
    onRequireLogin: () -> Unit = {},
    onEditClick: ((Place) -> Unit)? = null,
    onDeleted: (() -> Unit)? = null,
    currentUserId: String? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isWishlisted by remember { mutableStateOf(isWishlistedInitially) }
    var isVisited by remember { mutableStateOf(isVisitedInitially) }
    var liveVisitors by remember { mutableIntStateOf(place.visitorsCount) }
    var liveRating by remember { mutableDoubleStateOf(place.displayRating) }
    var liveRatingsCount by remember { mutableIntStateOf(place.ratingsCount) }
    var userStars by remember { mutableIntStateOf(0) }
    var ratingFeedback by remember { mutableStateOf("") }
    var ratingSubmitting by remember { mutableStateOf(false) }
    var actionMsg by remember { mutableStateOf<String?>(null) }
    var menuExpanded by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }

    val isOwner = remember(place, currentUserId, tokenManager) {
        val uid = currentUserId ?: tokenManager?.userId
        !uid.isNullOrBlank() && place.addedById.isNotBlank() && place.addedById == uid
    }
    val isAdmin = tokenManager?.isAdmin == true
    // Only admins can edit / delete places
    val canManage = isAdmin

    fun authOr(action: suspend (String) -> Unit) {
        val token = tokenManager?.accessToken
        if (token.isNullOrBlank() || tokenManager?.isLoggedIn != true) {
            onRequireLogin()
            return
        }
        scope.launch {
            try {
                action(token)
            } catch (e: Exception) {
                actionMsg = e.message
            }
        }
    }

    fun openMaps() {
        val query = place.location
            ?.takeIf { it.isNotBlank() }
            ?: place.locationLine.ifBlank { place.name }
        val gmm = Uri.parse("geo:0,0?q=${Uri.encode(query)}")
        val mapIntent = Intent(Intent.ACTION_VIEW, gmm).apply {
            setPackage("com.google.android.apps.maps")
        }
        try {
            context.startActivity(mapIntent)
        } catch (_: Exception) {
            val web = Uri.parse(
                "https://www.google.com/maps/search/?api=1&query=${Uri.encode(query)}"
            )
            context.startActivity(Intent(Intent.ACTION_VIEW, web))
        }
    }


    fun performDelete() {
        val token = tokenManager?.accessToken ?: return
        deleting = true
        scope.launch {
            try {
                RetrofitInstance.adminApi.deletePlace("Bearer $token", place._id)
                actionMsg = "Place deleted"
                onDeleted?.invoke()
            } catch (e: Exception) {
                actionMsg = e.message ?: "Delete failed"
            } finally {
                deleting = false
                showDeleteConfirm = false
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { if (!deleting) showDeleteConfirm = false },
            title = { Text("Delete place?") },
            text = {
                Text(
                    "This will permanently remove \"${place.name}\"."

                )
            },
            confirmButton = {
                TextButton(
                    onClick = { performDelete() },
                    enabled = !deleting
                ) {
                    Text(if (deleting) "Deleting…" else "Delete", color = Color(0xFFC62828))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }, enabled = !deleting) {
                    Text("Cancel")
                }
            }
        )
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(Color(0xFFF7F8FA))) {
        val heroHeight = maxHeight * 0.5f
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // ── Hero (≈50% of screen) ──────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(heroHeight)
            ) {
                AsyncImage(
                    model = place.imageUrl,
                    contentDescription = place.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Gradient for readability
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.35f),
                                    Color.Transparent,
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.65f)
                                )
                            )
                        )
                )

                // Top bar: back | share + manage menu
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 12.dp)
                        .align(Alignment.TopCenter),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircleIconButton(onClick = onBackClick) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.Black
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (canManage) {
                            Box {
                                CircleIconButton(onClick = { menuExpanded = true }) {
                                    Icon(
                                        Icons.Default.MoreVert,
                                        contentDescription = "Manage",
                                        tint = Color.Black
                                    )
                                }
                                DropdownMenu(
                                    expanded = menuExpanded,
                                    onDismissRequest = { menuExpanded = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Edit place") },
                                        onClick = {
                                            menuExpanded = false
                                            onEditClick?.invoke(place)
                                        },
                                        leadingIcon = {
                                            Icon(Icons.Default.Edit, contentDescription = null)
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Delete place", color = Color(0xFFC62828)) },
                                        onClick = {
                                            menuExpanded = false
                                            showDeleteConfirm = true
                                        },
                                        leadingIcon = {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = null,
                                                tint = Color(0xFFC62828)
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Country + title overlay at bottom of hero
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(horizontal = 20.dp, vertical = 24.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.95f),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = place.countryOrFallback,
                            color = Color.White.copy(alpha = 0.95f),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = place.name,
                        color = Color.White,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif,
                        lineHeight = 40.sp
                    )
                }
            }

            // ── Content sheet ──────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Color(0xFFF7F8FA),
                        RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp)
                    )
                    .padding(top = 16.dp, bottom = 28.dp)
            ) {
                // Horizontal info pills
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    InfoPill(
                        icon = { Icon(Icons.Default.Star, null, tint = Color(0xFFFFB300), modifier = Modifier.size(18.dp)) },
                        label = "Rating",
                        value = String.format("%.1f", liveRating)
                    )
                    InfoPill(
                        icon = { Icon(Icons.Default.People, null, tint = Color(0xFF1976D2), modifier = Modifier.size(18.dp)) },
                        label = "Visitors",
                        value = formatCount(liveVisitors)
                    )
                    if (liveRatingsCount > 0) {
                        InfoPill(
                            icon = null,
                            label = "Reviews",
                            value = liveRatingsCount.toString()
                        )
                    }
                }

                // Wishlist + Visited actions
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ActionChip(
                        selected = isWishlisted,
                        selectedColor = Color(0xFFE91E63),
                        label = if (isWishlisted) "Wishlisted" else "Wishlist",
                        icon = if (isWishlisted) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        onClick = {
                            authOr { token ->
                                if (isWishlisted) {
                                    RetrofitInstance.placeApi.removeWishlist("Bearer $token", place._id)
                                    isWishlisted = false
                                    actionMsg = "Removed from wishlist"
                                } else {
                                    RetrofitInstance.placeApi.addWishlist("Bearer $token", place._id)
                                    isWishlisted = true
                                    actionMsg = "Added to wishlist"
                                }
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                    ActionChip(
                        selected = isVisited,
                        selectedColor = Color(0xFF2E7D32),
                        label = if (isVisited) "Visited" else "Mark visited",
                        icon = if (isVisited) Icons.Default.CheckCircle else Icons.Outlined.CheckCircle,
                        onClick = {
                            authOr { token ->
                                if (isVisited) {
                                    val res = RetrofitInstance.authApi.removeVisitedPlace("Bearer $token", place._id)
                                    isVisited = false
                                    res.visitorsCount?.let { liveVisitors = it }
                                    actionMsg = "Removed from visited"
                                } else {
                                    val res = RetrofitInstance.authApi.addVisitedPlace("Bearer $token", place._id)
                                    isVisited = true
                                    res.visitorsCount?.let { liveVisitors = it }
                                    actionMsg = "Marked as visited"
                                }
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                if (actionMsg != null) {
                    Text(
                        text = actionMsg!!,
                        fontSize = 13.sp,
                        color = Color(0xFF1565C0),
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                }

                // ── Rate this place ────────────────────────────────
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Rate this place",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1A1A1A)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Your rating helps other travelers. You can update it anytime.",
                            fontSize = 13.sp,
                            color = Color(0xFF78909C)
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            for (i in 1..5) {
                                Icon(
                                    imageVector = if (i <= userStars) Icons.Default.Star else Icons.Outlined.Star,
                                    contentDescription = "$i stars",
                                    tint = if (i <= userStars) Color(0xFFFFB300) else Color(0xFFB0BEC5),
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clickable { userStars = i }
                                )
                            }
                            if (userStars > 0) {
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "$userStars / 5",
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF455A64)
                                )
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = ratingFeedback,
                            onValueChange = { ratingFeedback = it },
                            label = { Text("Feedback (optional)") },
                            placeholder = { Text("What did you like or improve?") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            minLines = 2,
                            maxLines = 4
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = {
                                if (userStars < 1) {
                                    actionMsg = "Tap stars to choose a rating first"
                                    return@Button
                                }
                                authOr { token ->
                                    ratingSubmitting = true
                                    try {
                                        val res = RetrofitInstance.placeApi.ratePlace(
                                            bearer = "Bearer $token",
                                            id = place._id,
                                            body = RatePlaceRequest(
                                                value = userStars,
                                                feedback = ratingFeedback.trim().ifBlank { null }
                                            )
                                        )
                                        liveRating = res.averageRating
                                        liveRatingsCount = res.ratingsCount
                                        if (res.visitorsCount > 0) liveVisitors = res.visitorsCount
                                        actionMsg = res.message ?: "Thanks for your rating!"
                                    } finally {
                                        ratingSubmitting = false
                                    }
                                }
                            },
                            enabled = !ratingSubmitting && userStars > 0,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB300))
                        ) {
                            Text(
                                if (ratingSubmitting) "Submitting…" else "Submit rating",
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF3E2723)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Description
                Text(
                    text = "About",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1A1A1A),
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = place.description?.takeIf { it.isNotBlank() }
                        ?: place.shortDescription
                        ?: "No description yet.",
                    fontSize = 15.sp,
                    lineHeight = 24.sp,
                    color = Color(0xFF4A5568),
                    modifier = Modifier.padding(horizontal = 20.dp)
                )

                Spacer(Modifier.height(24.dp))

                // Location
                Text(
                    text = "Location",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1A1A1A),
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
                Spacer(Modifier.height(10.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clickable { openMaps() },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE3F2FD)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = Color(0xFF1976D2)
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = place.location?.takeIf { it.isNotBlank() }
                                    ?: place.locationLine.ifBlank { place.name },
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF212121),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                "Tap to open in Maps",
                                fontSize = 12.sp,
                                color = Color(0xFF1976D2)
                            )
                        }
                        Icon(
                            Icons.Default.Map,
                            contentDescription = null,
                            tint = Color(0xFF90CAF9)
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = { openMaps() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))
                ) {
                    Icon(Icons.Default.Map, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text("Open in Google Maps", fontSize = 16.sp)
                }

                // Owner / admin quick edit bar
                if (canManage) {
                    Spacer(Modifier.height(20.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { onEditClick?.invoke(place) },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF37474F))
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.size(6.dp))
                            Text("Edit")
                        }
                        Button(
                            onClick = { showDeleteConfirm = true },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.size(6.dp))
                            Text("Delete")
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun CircleIconButton(
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.92f))
    ) {
        content()
    }
}

@Composable
private fun InfoPill(
    icon: (@Composable () -> Unit)?,
    label: String,
    value: String
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) {
                    icon()
                    Spacer(Modifier.width(6.dp))
                }
                Text(label, fontSize = 12.sp, color = Color(0xFF78909C))
            }
            Text(
                text = value,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF212121),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ActionChip(
    selected: Boolean,
    selectedColor: Color,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = if (selected) selectedColor.copy(alpha = 0.12f) else Color.White
    val fg = if (selected) selectedColor else Color(0xFF455A64)
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = bg),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(label, color = fg, fontWeight = FontWeight.Medium, fontSize = 14.sp)
        }
    }
}

private fun formatCount(n: Int): String {
    return when {
        n >= 1_000_000 -> String.format("%.1fM", n / 1_000_000.0)
        n >= 1_000 -> String.format("%.1fK", n / 1_000.0)
        else -> n.toString()
    }
}
