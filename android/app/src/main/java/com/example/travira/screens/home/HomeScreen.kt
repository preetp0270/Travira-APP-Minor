package com.example.travira.screens.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.travira.components.AppCard
import com.example.travira.model.Place
import com.example.travira.ui.theme.TraviraBrandFont
import kotlin.math.roundToInt

private val BrandBlue = Color(0xFF1565C0)

/** Top floating pill is ~2× the bottom bar pill footprint. */
private val TopPillHorizontalPadding = 24.dp
/**
 * Extra space above the floating top pill (below the status bar).
 * Change this value manually to tune the gap — e.g. 28.dp, 36.dp, 44.dp.
 * File: screens/home/HomeScreen.kt → TopPillTopGap
 */
private val TopPillTopGap = 20.dp
private val TopPillShape = RoundedCornerShape(36.dp)
/**
 * Clearance under the pill so list content does not sit under it.
 * If you increase [TopPillTopGap] a lot, bump this slightly too.
 */
private val TopContentClearance = 96.dp

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
    var selectedCategory by remember { mutableStateOf("Popular") }
    val categories = listOf(
        "Popular", "Beach", "Mountains", "Heritage", "Foodie Spots", "Hidden Gems"
    )
    val pullState = rememberPullToRefreshState()
    val listState = rememberLazyListState()
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    // Scroll-aware: hide on scroll down, show on scroll up (not permanently sticky)
    var headerVisible by remember { mutableStateOf(true) }
    var accumulated by remember { mutableFloatStateOf(0f) }

    val showScrollToTop by remember {
        derivedStateOf {
            headerVisible && (listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 150)
        }
    }
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val dy = available.y
                // Ignore tiny jitter
                if (dy < -2f) {
                    // content scrolling up → finger down → hide header
                    accumulated += dy
                    if (accumulated < -24f) {
                        headerVisible = false
                        accumulated = 0f
                    }
                } else if (dy > 2f) {
                    // content scrolling down → finger up → show header
                    accumulated += dy
                    if (accumulated > 16f) {
                        headerVisible = true
                        accumulated = 0f
                    }
                }
                return Offset.Zero
            }
        }
    }

    val headerOffsetPx by animateFloatAsState(
        targetValue = if (headerVisible) 0f else with(density) { (-130).dp.toPx() },
        animationSpec = tween(durationMillis = 280),
        label = "headerOffset"
    )

    val filtered = remember(places, query, selectedCategory) {
        val q = query.trim()
        var list = places
        if (q.isNotEmpty()) {
            list = list.filter { p ->
                listOfNotNull(
                    p.name, p.city, p.state, p.country, p.location,
                    p.shortDescription, p.description
                ).any { it.contains(q, ignoreCase = true) }
            }
        }
        if (selectedCategory != "Popular") {
            val keywords = when (selectedCategory) {
                "Beach" -> listOf("beach", "coast", "island", "sea", "bay", "ocean")
                "Mountains" -> listOf("mountain", "hill", "peak", "alps", "trek", "alpine")
                "Heritage" -> listOf("heritage", "temple", "fort", "palace", "museum", "historic", "taj")
                "Foodie Spots" -> listOf("food", "cuisine", "restaurant", "street food", "market")
                "Hidden Gems" -> listOf("hidden", "secret", "village", "quiet", "offbeat")
                else -> emptyList()
            }
            if (keywords.isNotEmpty()) {
                val matched = list.filter { p ->
                    val blob = listOfNotNull(
                        p.name, p.shortDescription, p.description, p.city, p.country, p.location
                    ).joinToString(" ").lowercase()
                    keywords.any { blob.contains(it) }
                }
                if (matched.isNotEmpty()) list = matched
            }
        }
        list
    }

    fun openRandomPlace() {
        val pool = if (filtered.isNotEmpty()) filtered else places
        if (pool.isEmpty()) return
        onPlaceClick(pool.random())
    }

    val colors = MaterialTheme.colorScheme

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
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
                    modifier = Modifier
                        .fillMaxSize()
                        .nestedScroll(nestedScrollConnection)
                ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Space so first cards sit below the floating top pill
                        item {
                            Spacer(
                                modifier = Modifier
                                    .statusBarsPadding()
                                    .height(TopPillTopGap + TopContentClearance)
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
                            // Greeting — compact
                            item {
                                val greetName = userName?.trim()?.takeIf { it.isNotBlank() }?.substringBefore(" ")
                                    ?: "explorer"
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 20.dp)
                                ) {
                                    Text(
                                        text = "Where to next,",
                                        fontSize = 13.sp,
                                        color = colors.onBackground.copy(alpha = 0.55f)
                                    )
                                    Text(
                                        text = "$greetName?",
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Serif,
                                        color = colors.onBackground
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = "Find places you’ll love — search or pick a category.",
                                        fontSize = 12.sp,
                                        color = colors.onBackground.copy(alpha = 0.6f),
                                        lineHeight = 16.sp
                                    )
                                }
                            }

                            // Category chips (Stitch)
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState())
                                        .padding(horizontal = 16.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    categories.forEach { cat ->
                                        val selected = cat == selectedCategory
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(20.dp))
                                                .background(
                                                    if (selected) Color(0xFFE0F2FE) else Color.White
                                                )
                                                .border(
                                                    width = if (selected) 1.5.dp else 1.dp,
                                                    color = if (selected) Color(0xFF0284C7) else Color(0xFFCFD8DC),
                                                    shape = RoundedCornerShape(20.dp)
                                                )
                                                .clickable { selectedCategory = cat }
                                                .padding(horizontal = 14.dp, vertical = 10.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                if (cat == "Popular" && selected) {
                                                    Icon(
                                                        Icons.Default.AutoAwesome,
                                                        contentDescription = null,
                                                        tint = Color(0xFF0284C7),
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Spacer(Modifier.width(4.dp))
                                                }
                                                Text(
                                                    text = cat,
                                                    fontSize = 13.sp,
                                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                                                    color = if (selected) Color(0xFF0284C7) else Color(0xFF546E7A)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 20.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (query.isBlank()) "Curated Journeys" else "Results",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Serif,
                                        color = colors.onBackground
                                    )
                                    Text(
                                        text = "${filtered.size} Destinations",
                                        fontSize = 13.sp,
                                        color = colors.onBackground.copy(alpha = 0.55f)
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

                // Floating glassy top pill — hides on scroll down, shows on scroll up
                HomeTopPill(
                    query = query,
                    onQueryChange = { query = it },
                    onRandomClick = { openRandomPlace() },
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                        .padding(top = TopPillTopGap)
                        .offset { IntOffset(0, headerOffsetPx.roundToInt()) }
                )
            }
        }

        AnimatedVisibility(
            visible = showScrollToTop,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(
                    end = 20.dp,
                    bottom = if (isAdmin) 164.dp else 96.dp
                )
        ) {
            SmallFloatingActionButton(
                onClick = {
                    coroutineScope.launch {
                        listState.animateScrollToItem(0)
                    }
                },
                containerColor = BrandBlue,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowUp,
                    contentDescription = "Scroll to top"
                )
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

/**
 * Glassy floating top pill (~2× bottom bar size).
 * Row 1: Travira name (left) + logo (right)
 * Row 2: search bar + random button
 */
@Composable
private fun HomeTopPill(
    query: String,
    onQueryChange: (String) -> Unit,
    onRandomClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = TopPillHorizontalPadding),
        horizontalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 14.dp,
                    shape = TopPillShape
                ),
            shape = TopPillShape,
            color = Color(0xFF1565C0).copy(alpha = 0.22f),
            tonalElevation = 0.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    // Glass layer (matches bottom bar frosted style)
                    .background(
                        Color.White.copy(alpha = 0.58f),
                        TopPillShape
                    )
                    .border(
                        BorderStroke(1.dp, Color.White.copy(alpha = 0.65f)),
                        TopPillShape
                    )
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                // Part 1 — "Travira" title
                BrandTitleFullWidth()

                // Part 2 — search + random
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(Color.White.copy(alpha = 0.72f))
                            .border(
                                BorderStroke(1.dp, BrandBlue.copy(alpha = 0.18f)),
                                RoundedCornerShape(22.dp)
                            )
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = BrandBlue,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        BasicTextField(
                            value = query,
                            onValueChange = onQueryChange,
                            singleLine = true,
                            textStyle = TextStyle(
                                fontSize = 14.sp,
                                color = Color(0xFF0D1B2A),
                                fontWeight = FontWeight.Medium
                            ),
                            cursorBrush = SolidColor(BrandBlue),
                            modifier = Modifier.weight(1f),
                            decorationBox = { inner ->
                                if (query.isEmpty()) {
                                    Text(
                                        "Search places, cities…",
                                        color = Color(0xFF90A4AE),
                                        fontSize = 13.sp
                                    )
                                }
                                inner()
                            }
                        )
                    }

                    IconButton(
                        onClick = onRandomClick,
                        modifier = Modifier
                            .size(44.dp)
                            .shadow(6.dp, RoundedCornerShape(22.dp))
                            .clip(RoundedCornerShape(22.dp))
                            .background(BrandBlue)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Casino,
                            contentDescription = "Surprise me — random place",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BrandTitleFullWidth(
    modifier: Modifier = Modifier,
    text: String = "Travira",
    color: Color = BrandBlue
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp),
        contentAlignment = Alignment.Center
    ) {
        val textMeasurer = rememberTextMeasurer()
        val targetWidthPx = constraints.maxWidth.toFloat()

        val baseStyle = TextStyle(
            fontFamily = TraviraBrandFont,
            fontWeight = FontWeight.Bold,
            color = color,
            platformStyle = PlatformTextStyle(includeFontPadding = false)
        )

        val baseFontSize = 100.sp
        val measuredResult = textMeasurer.measure(
            text = text,
            style = baseStyle.copy(fontSize = baseFontSize)
        )
        val measuredWidthPx = measuredResult.size.width.toFloat()

        val fontSize = if (measuredWidthPx > 0f && targetWidthPx > 0f) {
            val scale = (targetWidthPx * 0.94f) / measuredWidthPx
            minOf(100 * scale, 38f).sp
        } else {
            36.sp
        }

        Text(
            text = text,
            style = baseStyle.copy(
                fontSize = fontSize,
                lineHeight = fontSize,
                lineHeightStyle = LineHeightStyle(
                    alignment = LineHeightStyle.Alignment.Bottom,
                    trim = LineHeightStyle.Trim.Both
                ),
                textAlign = TextAlign.Center
            ),
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.padding(bottom = 0.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeTopPillPreview() {
    MaterialTheme {
        Box(
            modifier = Modifier
                .background(Color(0xFFE0E0E0))
                .padding(16.dp)
        ) {
            HomeTopPill(
                query = "",
                onQueryChange = {},
                onRandomClick = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "Home – Loaded", showSystemUi = true)
@Composable
private fun HomeScreenPreview() {
    MaterialTheme {
        HomeScreen(
            places = listOf(
                Place(
                    _id = "1",
                    name = "Tashkent Amir Timur Square",
                    city = "Tashkent",
                    country = "Uzbekistan",
                    location = "Tashkent, Uzbekistan",
                    rating = 3.0,
                    averageRating = 3.0,
                    ratingsCount = 92,
                    visitorsCount = 902
                ),
                Place(
                    _id = "2",
                    name = "Himalayan Viewpoint",
                    city = "Manali",
                    country = "India",
                    location = "Manali, Himachal Pradesh",
                    rating = 4.5,
                    averageRating = 4.5,
                    ratingsCount = 120,
                    visitorsCount = 1500
                )
            ),
            isLoading = false,
            onPlaceClick = {},
            userName = "Preet"
        )
    }
}

@Preview(showBackground = true, name = "Home – Loading")
@Composable
private fun HomeScreenLoadingPreview() {
    MaterialTheme {
        HomeScreen(
            places = emptyList(),
            isLoading = true,
            onPlaceClick = {}
        )
    }
}
