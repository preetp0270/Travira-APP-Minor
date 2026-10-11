package com.example.travira.screens.places

import android.content.*
import android.net.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.tooling.preview.*
import androidx.compose.ui.unit.*
import coil3.compose.*
import com.example.travira.auth.*
import com.example.travira.model.*
import com.example.travira.remote.*
import com.example.travira.ui.theme.*
import kotlinx.coroutines.*

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
    /** Notify parent so list/home shows live visitors + rating for all users after refresh */
    onStatsChanged: ((placeId: String, visitorsCount: Int, averageRating: Double, ratingsCount: Int) -> Unit)? = null
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

    val colors = MaterialTheme.colorScheme

    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(colors.background)) {
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

                // Full address + title overlay at bottom of hero
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(horizontal = 20.dp, vertical = 20.dp)
                ) {
                    val addressLine = place.locationLine.ifBlank {
                        place.location.orEmpty()
                    }.ifBlank {
                        place.countryOrFallback
                    }
                    if (addressLine.isNotBlank() && addressLine != "—") {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.95f),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = addressLine,
                                color = Color.White.copy(alpha = 0.95f),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 2
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                    }
                    Text(
                        text = place.name,
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif,
                        lineHeight = 36.sp
                    )
                }
            }

            // ── Content sheet (follows light/dark theme) ───────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        colors.background,
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
                Spacer(Modifier.height(8.dp))

                // Description
                Text(
                    text = "About",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onBackground,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = place.description?.takeIf { it.isNotBlank() }
                        ?: place.shortDescription
                        ?: "No description yet.",
                    fontSize = 14.sp,
                    lineHeight = 21.sp,
                    color = colors.onBackground.copy(alpha = 0.72f),
                    modifier = Modifier.padding(horizontal = 20.dp)
                )

                Spacer(Modifier.height(14.dp))

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
                                    res.visitorsCount?.let {
                                        liveVisitors = it
                                        onStatsChanged?.invoke(place._id, it, liveRating, liveRatingsCount)
                                    }
                                    actionMsg = "Removed from visited"
                                } else {
                                    val res = RetrofitInstance.authApi.addVisitedPlace("Bearer $token", place._id)
                                    isVisited = true
                                    res.visitorsCount?.let {
                                        liveVisitors = it
                                        onStatsChanged?.invoke(place._id, it, liveRating, liveRatingsCount)
                                    }
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

                // Single location card → opens system Maps app
                Text(
                    text = "Location",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onBackground,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
                Spacer(Modifier.height(8.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clickable { openMaps() },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = colors.surface.copy(alpha = 0.96f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        Color(0xFF90CAF9).copy(alpha = 0.55f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .background(colors.surface)
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFF1565C0), Color(0xFF42A5F5))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = place.location?.takeIf { it.isNotBlank() }
                                    ?: place.locationLine.ifBlank { place.name },
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.onSurface,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                "Open in Maps",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF1565C0)
                            )
                        }
                        Icon(
                            Icons.Default.Map,
                            contentDescription = "Open maps",
                            tint = Color(0xFF1565C0),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                // ── Rate this place ────────────────────────────────

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Rate this place",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.onSurface
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Your rating helps other travelers. You can update it anytime.",
                            fontSize = 13.sp,
                            color = colors.onSurface.copy(alpha = 0.55f)
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
                                        // Always take server visitorsCount (may be 0 — that is valid)
                                        liveVisitors = res.visitorsCount
                                        onStatsChanged?.invoke(
                                            place._id,
                                            liveVisitors,
                                            liveRating,
                                            liveRatingsCount
                                        )
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
    val colors = MaterialTheme.colorScheme
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
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
                Text(label, fontSize = 12.sp, color = colors.onSurface.copy(alpha = 0.55f))
            }
            Text(
                text = value,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.onSurface,
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
    val scheme = MaterialTheme.colorScheme
    val bg = if (selected) selectedColor.copy(alpha = 0.12f) else scheme.surface
    val fg = if (selected) selectedColor else scheme.onSurface.copy(alpha = 0.75f)
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


@Preview(showBackground = true, name = "Place Detail", showSystemUi = true)
@Composable
private fun PlaceScreenPreview() {
    TraviraTheme {
        PlaceScreen(
            place = Place(
                _id = "1",
                name = "Amir Timur Square",
                shortDescription = "Historic square in the heart of Tashkent",
                description = "A landmark plaza featuring a bronze statue of Amir Timur.",
                city = "Tashkent",
                state = "Tashkent",
                country = "Uzbekistan",
                location = "Tashkent, Uzbekistan",
                rating = 4.2,
                averageRating = 4.2,
                ratingsCount = 92,
                visitorsCount = 902
            ),
            onBackClick = {}
        )
    }
}
