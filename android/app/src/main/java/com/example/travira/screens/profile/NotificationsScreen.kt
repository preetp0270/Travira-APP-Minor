package com.example.travira.screens.profile

import androidx.compose.ui.tooling.preview.Preview
import com.example.travira.ui.theme.TraviraTheme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.travira.auth.TokenManager
import com.example.travira.model.NotificationItem
import com.example.travira.remote.ApiErrorHelper
import com.example.travira.remote.RetrofitInstance
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    tokenManager: TokenManager,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var items by remember { mutableStateOf<List<NotificationItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    fun load() {
        val token = tokenManager.accessToken
        if (token.isNullOrBlank()) {
            error = "Please log in"
            loading = false
            return
        }
        loading = true
        error = null
        scope.launch {
            try {
                val res = RetrofitInstance.authApi.getNotifications("Bearer $token")
                items = res.notifications.sortedByDescending { it.createdAt }
            } catch (e: Exception) {
                error = ApiErrorHelper.message(e)
            } finally {
                loading = false
            }
        }
    }

    fun markAllRead() {
        val token = tokenManager.accessToken ?: return
        scope.launch {
            try {
                val res = RetrofitInstance.authApi.markNotificationsRead("Bearer $token")
                items = res.notifications
            } catch (_: Exception) {
            }
        }
    }

    LaunchedEffect(Unit) { load() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = { Text("Notifications") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                if (items.any { !it.read }) {
                    Button(
                        onClick = { markAllRead() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0)),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text("Mark all read", fontSize = 12.sp)
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color(0xFF1565C0),
                titleContentColor = Color.White,
                navigationIconContentColor = Color.White
            )
        )

        when {
            loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF1565C0))
                }
            }
            error != null -> {
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(error!!, color = Color.Gray)
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { load() }) { Text("Retry") }
                }
            }
            items.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No notifications yet", color = Color.Gray)
                }
            }
            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(items, key = { it.id.ifBlank { it.title + (it.createdAt ?: "") } }) { n ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (n.read) Color.White else Color(0xFFE3F2FD)
                                )
                                .padding(14.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF1565C0).copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Notifications,
                                    null,
                                    tint = Color(0xFF1565C0),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(Modifier.size(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    n.title,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp,
                                    color = Color(0xFF0D1B2A)
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(n.message, fontSize = 13.sp, color = Color(0xFF546E7A))
                                n.createdAt?.takeIf { it.isNotBlank() }?.let { at ->
                                    Spacer(Modifier.height(6.dp))
                                    Text(at, fontSize = 11.sp, color = Color(0xFF90A4AE))
                                }
                            }
                        }
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }
}


@Preview(showBackground = true, name = "Notifications", showSystemUi = true)
@Composable
private fun NotificationsScreenPreview() {
    val context = androidx.compose.ui.platform.LocalContext.current
    TraviraTheme {
        NotificationsScreen(
            tokenManager = com.example.travira.auth.TokenManager(context),
            onBack = {}
        )
    }
}

