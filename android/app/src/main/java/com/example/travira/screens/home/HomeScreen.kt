package com.example.travira.screens.home

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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

private val Teal = Color(0xFF1B6B63)
private val SoftBg = Color(0xFFF5F7F6)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    places: List<Place>,
    isLoading: Boolean,
    errorMessage: String? = null,
    onPlaceClick: (Place) -> Unit,
    onRetry: () -> Unit = {},
    onRefresh: () -> Unit = {},
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
                    CircularProgressIndicator(color = Teal)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = "Loading places...")
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
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage,
                        fontSize = 13.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(onClick = onRetry) { Text("Retry") }
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
                                        color = Color.Gray
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    if (query.isNotBlank()) {
                                        Button(onClick = { query = "" }) { Text("Clear search") }
                                    } else {
                                        Button(onClick = onRetry) { Text("Refresh") }
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
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Serif,
                                        color = Color(0xFF1A1A1A)
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

    }
}

@Composable
private fun HomeHeader(
    displayName: String,
    query: String,
    onQueryChange: (String) -> Unit,
    onRandomClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Hello, $displayName 👋",
                    fontSize = 14.sp,
                    color = Color(0xFF607D8B),
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Let's explore",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif,
                    color = Color(0xFF12201E),
                    lineHeight = 30.sp
                )
                Text(
                    text = "Discover places with Travira",
                    fontSize = 12.sp,
                    color = Teal,
                    fontWeight = FontWeight.Medium
                )
            }

            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Teal),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "T",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    fontFamily = FontFamily.Serif
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    tint = Teal,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = TextStyle(
                        fontSize = 14.sp,
                        color = Color(0xFF263238)
                    ),
                    cursorBrush = SolidColor(Teal),
                    modifier = Modifier.weight(1f),
                    decorationBox = { inner ->
                        if (query.isEmpty()) {
                            Text(
                                "Search places, cities…",
                                color = Color(0xFFB0BEC5),
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
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Teal)
            ) {
                Icon(
                    imageVector = Icons.Default.Casino,
                    contentDescription = "Surprise me — random place",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
