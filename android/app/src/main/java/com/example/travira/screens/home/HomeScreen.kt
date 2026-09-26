package com.example.travira.screens.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.travira.components.AppCard
import com.example.travira.model.Place

private val BrandBlue = Color(0xFF1565C0)
private val SoftBg = Color(0xFFF0F6FC)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    places: List<Place>,
    isLoading: Boolean,
    errorMessage: String? = null,
    onPlaceClick: (Place) -> Unit,
    onRetry: () -> Unit = {},
    onRefresh: () -> Unit = {},
    onAddClick: () -> Unit = {},
    isAdmin: Boolean = false,
    userName: String? = null,
    modifier: Modifier = Modifier
) {
    var query by remember { mutableStateOf("") }
    val pullState = rememberPullToRefreshState()

    val filtered = remember(places, query) {
        val q = query.trim()
        if (q.isEmpty()) places
        else {
            places.filter { p ->
                listOfNotNull(
                    p.name,
                    p.city,
                    p.state,
                    p.country,
                    p.location,
                    p.shortDescription,
                    p.description
                ).any { it.contains(q, ignoreCase = true) }
            }
        }
    }

    fun openRandomPlace() {
        val pool = if (filtered.isNotEmpty()) filtered else places
        if (pool.isEmpty()) return
        onPlaceClick(pool.random())
    }

    val displayName = userName?.takeIf { it.isNotBlank() }?.substringBefore(" ") ?: "Traveler"

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SoftBg)
    ) {
        when {
            isLoading && places.isEmpty() -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(color = BrandBlue)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = "Loading places...", color = Color(0xFF546E7A))
                }
            }

            !isLoading && places.isEmpty() && errorMessage != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Could not load places",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF0D1B2A)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage,
                        fontSize = 13.sp,
                        color = Color(0xFF78909C),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = onRetry,
                        colors = ButtonDefaults.buttonColors(containerColor = BrandBlue)
                    ) { Text("Retry") }
                }
            }

            else -> {
                PullToRefreshBox(
                    isRefreshing = isLoading,
                    onRefresh = onRefresh,
                    state = pullState,
                    modifier = Modifier.fillMaxSize()
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            HomeHeader(
                                displayName = displayName,
                                query = query,
                                onQueryChange = { query = it },
                                onRandomClick = { openRandomPlace() }
                            )
                        }

                        if (filtered.isEmpty()) {
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 48.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        if (query.isBlank()) "No places found"
                                        else "No matches for \"$query\"",
                                        color = Color(0xFF78909C)
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    if (query.isNotBlank()) {
                                        Button(
                                            onClick = { query = "" },
                                            colors = ButtonDefaults.buttonColors(containerColor = BrandBlue)
                                        ) { Text("Clear search") }
                                    } else {
                                        Button(
                                            onClick = onRetry,
                                            colors = ButtonDefaults.buttonColors(containerColor = BrandBlue)
                                        ) { Text("Refresh") }
                                    }
                                }
                            }
                        } else {
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 20.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (query.isBlank()) "Trending now" else "Results",
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Serif,
                                        color = Color(0xFF0D1B2A)
                                    )
                                    Text(
                                        text = "${filtered.size} place${if (filtered.size == 1) "" else "s"}",
                                        fontSize = 14.sp,
                                        color = Color(0xFF78909C)
                                    )
                                }
                            }

                            items(
                                items = filtered,
                                key = { it._id.ifBlank { it.name } }
                            ) { place ->
                                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                    AppCard(
                                        place = place,
                                        onClick = { onPlaceClick(place) },
                                        showWishlistHeart = false
                                    )
                                }
                            }
                        }

                        item { Spacer(modifier = Modifier.height(110.dp)) }
                    }
                }
            }
        }

        if (isAdmin) {
            FloatingActionButton(
                onClick = onAddClick,
                containerColor = BrandBlue,
                contentColor = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = 96.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add place"
                )
            }
        }
    }
}

@Composable
private fun HomeHeader(
    displayName: String,
    query: String,
    onQueryChange: (String) -> Unit,
    onRandomClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Upper body above search — gradient glass hero
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF0B1D2A),
                            Color(0xFF0D47A1),
                            Color(0xFF1565C0),
                            Color(0xFF42A5F5).copy(alpha = 0.85f)
                        )
                    )
                )
                .padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 22.dp)
        ) {
            // Soft orb
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.12f))
            )

            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Hello, $displayName 👋",
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.85f),
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Let's explore",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif,
                            color = Color.White,
                            lineHeight = 32.sp
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Discover places with Travira",
                            fontSize = 13.sp,
                            color = Color(0xFFBBDEFB),
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .shadow(10.dp, CircleShape)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f))
                            .border(1.5.dp, Color.White.copy(alpha = 0.55f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.TravelExplore,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(Modifier.height(18.dp))

                // Glass search row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color.White.copy(alpha = 0.22f))
                            .border(
                                BorderStroke(1.dp, Color.White.copy(alpha = 0.45f)),
                                RoundedCornerShape(18.dp)
                            )
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        BasicTextField(
                            value = query,
                            onValueChange = onQueryChange,
                            singleLine = true,
                            textStyle = TextStyle(
                                fontSize = 15.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Medium
                            ),
                            cursorBrush = SolidColor(Color.White),
                            modifier = Modifier.weight(1f),
                            decorationBox = { inner ->
                                if (query.isEmpty()) {
                                    Text(
                                        "Search places, cities…",
                                        color = Color.White.copy(alpha = 0.65f),
                                        fontSize = 14.sp
                                    )
                                }
                                inner()
                            }
                        )
                    }

                    IconButton(
                        onClick = onRandomClick,
                        modifier = Modifier
                            .size(50.dp)
                            .shadow(8.dp, RoundedCornerShape(16.dp))
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Casino,
                            contentDescription = "Surprise me — random place",
                            tint = BrandBlue,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}
