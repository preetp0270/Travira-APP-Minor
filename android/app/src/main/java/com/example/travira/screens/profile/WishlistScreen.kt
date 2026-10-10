package com.example.travira.screens.profile

import androidx.compose.ui.tooling.preview.Preview
import com.example.travira.ui.theme.TraviraTheme

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.travira.auth.TokenManager
import com.example.travira.components.AppCard
import com.example.travira.model.Place
import com.example.travira.remote.RetrofitInstance
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WishlistScreen(
    tokenManager: TokenManager,
    onBack: () -> Unit,
    onPlaceClick: (Place) -> Unit
) {
    val scope = rememberCoroutineScope()
    var places by remember { mutableStateOf<List<Place>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    fun load() {
        scope.launch {
            loading = true
            error = null
            try {
                val token = tokenManager.accessToken ?: throw Exception("Not logged in")
                val res = RetrofitInstance.placeApi.getWishlist("Bearer $token")
                places = res.wishlist
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
                title = { Text("Wishlist") },
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
                loading && places.isEmpty() -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                places.isEmpty() -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("No wishlist places yet", fontSize = 16.sp, color = Color.Gray)
                        if (error != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(error!!, fontSize = 12.sp, color = Color(0xFFC62828))
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item { Spacer(modifier = Modifier.height(8.dp)) }
                        items(places, key = { it._id.ifBlank { it.name } }) { place ->
                            AppCard(
                                place = place,
                                onClick = { onPlaceClick(place) },
                                showWishlistHeart = true,
                                isWishlisted = true,
                                onWishlistClick = {
                                    scope.launch {
                                        try {
                                            val token = tokenManager.accessToken ?: return@launch
                                            RetrofitInstance.placeApi.removeWishlist(
                                                "Bearer $token",
                                                place._id
                                            )
                                            places = places.filter { it._id != place._id }
                                        } catch (_: Exception) { }
                                    }
                                }
                            )
                        }
                        item { Spacer(modifier = Modifier.height(24.dp)) }
                    }
                }
            }
        }
    }
}


@Preview(showBackground = true, name = "Wishlist", showSystemUi = true)
@Composable
private fun WishlistScreenPreview() {
    val context = androidx.compose.ui.platform.LocalContext.current
    TraviraTheme {
        WishlistScreen(
            tokenManager = com.example.travira.auth.TokenManager(context),
            onBack = {},
            onPlaceClick = {}
        )
    }
}

