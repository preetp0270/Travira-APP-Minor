package com.example.travira.screens.profile

import androidx.compose.ui.tooling.preview.Preview
import com.example.travira.ui.theme.TraviraTheme

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.travira.auth.TokenManager
import com.example.travira.model.Place
import com.example.travira.remote.MyReviewItem
import com.example.travira.remote.RetrofitInstance
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewsScreen(
    tokenManager: TokenManager,
    onBack: () -> Unit,
    onPlaceClick: (Place) -> Unit
) {
    val scope = rememberCoroutineScope()
    var reviews by remember { mutableStateOf<List<MyReviewItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    fun load() {
        scope.launch {
            loading = true
            error = null
            try {
                val token = tokenManager.accessToken ?: throw Exception("Not logged in")
                val res = RetrofitInstance.authApi.getMyRatings("Bearer $token")
                reviews = res.reviews
            } catch (e: Exception) {
                error = e.message
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(Unit) { load() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (reviews.isNotEmpty()) "My Reviews (${reviews.size})" else "My Reviews"
                    )
                },
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
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                loading && reviews.isEmpty() -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                reviews.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "No reviews yet",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.Gray
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Open a place and rate it with stars to see it here.",
                            fontSize = 13.sp,
                            color = Color(0xFF90A4AE)
                        )
                        if (error != null) {
                            Spacer(Modifier.height(8.dp))
                            Text(error!!, fontSize = 12.sp, color = Color(0xFFC62828))
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item { Spacer(Modifier.height(8.dp)) }
                        items(reviews, key = { it.placeId + it.myRating }) { review ->
                            ReviewCard(
                                review = review,
                                onClick = {
                                    onPlaceClick(
                                        Place(
                                            _id = review.placeId,
                                            name = review.name,
                                            shortDescription = review.shortDescription,
                                            city = review.city,
                                            state = review.state,
                                            country = review.country,
                                            location = review.location,
                                            imageUrl = review.imageUrl,
                                            averageRating = review.averageRating,
                                            visitorsCount = review.visitorsCount
                                        )
                                    )
                                }
                            )
                        }
                        item { Spacer(Modifier.height(24.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReviewCard(
    review: MyReviewItem,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        if (review.imageUrl.isNotBlank()) {
            AsyncImage(
                model = review.imageUrl,
                contentDescription = review.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFE3F2FD))
            )
        } else {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFE3F2FD)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Star,
                    contentDescription = null,
                    tint = Color(0xFFFFB300),
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = review.name,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = Color(0xFF0D1B2A),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (review.locationLine.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = review.locationLine,
                    fontSize = 12.sp,
                    color = Color(0xFF78909C),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                for (i in 1..5) {
                    Icon(
                        imageVector = if (i <= review.myRating) Icons.Default.Star else Icons.Outlined.Star,
                        contentDescription = null,
                        tint = if (i <= review.myRating) Color(0xFFFFB300) else Color(0xFFCFD8DC),
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(Modifier.width(6.dp))
                Text(
                    "${review.myRating}/5",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF455A64)
                )
            }
            if (review.myFeedback.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = review.myFeedback,
                    fontSize = 13.sp,
                    color = Color(0xFF546E7A),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 18.sp
                )
            }
        }
    }
}


@Preview(showBackground = true, name = "My Reviews", showSystemUi = true)
@Composable
private fun ReviewsScreenPreview() {
    val context = androidx.compose.ui.platform.LocalContext.current
    TraviraTheme {
        ReviewsScreen(
            tokenManager = com.example.travira.auth.TokenManager(context),
            onBack = {},
            onPlaceClick = {}
        )
    }
}

