package com.example.travira.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.travira.auth.TokenManager
import com.example.travira.model.Place
import com.example.travira.model.User
import com.example.travira.remote.RetrofitInstance
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    tokenManager: TokenManager,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var tab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Places", "Users")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admin Dashboard") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0D47A1),
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
                .background(Color(0xFFF5F7FA))
        ) {
            TabRow(
                selectedTabIndex = tab,
                containerColor = Color.White
            ) {
                tabs.forEachIndexed { i, title ->
                    Tab(
                        selected = tab == i,
                        onClick = { tab = i },
                        text = { Text(title) },
                        icon = {
                            Icon(
                                if (i == 0) Icons.Default.Place else Icons.Default.People,
                                contentDescription = null
                            )
                        }
                    )
                }
            }

            when (tabs.getOrNull(tab)) {
                "Places" -> AdminPlacesTab(tokenManager)
                "Users" -> AdminUsersTab(tokenManager)
            }
        }
    }
}

@Composable
private fun AdminPlacesTab(tokenManager: TokenManager) {
    val scope = rememberCoroutineScope()
    var places by remember { mutableStateOf<List<Place>>(emptyList()) }
    var total by remember { mutableStateOf(0) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var selected by remember { mutableStateOf<Place?>(null) }

    fun load() {
        scope.launch {
            loading = true
            error = null
            try {
                val token = tokenManager.accessToken ?: return@launch
                val res = RetrofitInstance.adminApi.getAllPlaces("Bearer $token")
                places = res.places
                total = res.counts?.total ?: res.places.size
            } catch (e: Exception) {
                error = e.message
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(Unit) { load() }

    if (selected != null) {
        AdminPlaceDetail(
            tokenManager = tokenManager,
            place = selected!!,
            onBack = { selected = null; load() }
        )
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "$total places",
            modifier = Modifier.padding(12.dp),
            fontWeight = FontWeight.Medium,
            color = Color.Gray
        )

        when {
            loading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            error != null -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(error ?: "", color = Color.Red)
            }
            else -> LazyColumn(
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(places, key = { it._id }) { place ->
                    AdminPlaceCard(place) { selected = place }
                }
            }
        }
    }
}

@Composable
private fun AdminPlaceCard(place: Place, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = place.imageUrl,
                contentDescription = place.name,
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(10.dp)),
                contentScale = ContentScale.Crop
            )
            Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                Text(place.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(
                    listOfNotNull(place.city, place.state).joinToString(", "),
                    fontSize = 13.sp,
                    color = Color.Gray
                )
                Text(
                    "★ ${place.displayRating}  ·  ${place.visitorsCount} visitors",
                    fontSize = 12.sp,
                    color = Color(0xFF607D8B)
                )
            }
        }
    }
}

@Composable
private fun AdminPlaceDetail(
    tokenManager: TokenManager,
    place: Place,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var message by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
        }
        AsyncImage(
            model = place.imageUrl,
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(16.dp)),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(place.name, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(listOfNotNull(place.city, place.state, place.country).joinToString(", "), color = Color.Gray)
        Spacer(modifier = Modifier.height(8.dp))
        Text(place.shortDescription ?: place.description ?: "", fontSize = 14.sp)
        Spacer(modifier = Modifier.height(12.dp))
        Text("Rating: ${place.displayRating}  •  Visitors: ${place.visitorsCount}", fontSize = 13.sp)

        message?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(it, color = Color(0xFF1565C0))
        }
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = {
                scope.launch {
                    try {
                        val token = tokenManager.accessToken ?: return@launch
                        RetrofitInstance.adminApi.deletePlace("Bearer $token", place._id)
                        onBack()
                    } catch (e: Exception) {
                        message = e.message
                    }
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB71C1C))
        ) {
            Text("Delete place")
        }
    }
}

@Composable
private fun AdminUsersTab(tokenManager: TokenManager) {
    val scope = rememberCoroutineScope()
    var users by remember { mutableStateOf<List<User>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var selected by remember { mutableStateOf<User?>(null) }
    var detailNote by remember { mutableStateOf<String?>(null) }
    var editName by remember { mutableStateOf("") }
    var editEmail by remember { mutableStateOf("") }
    var editPhone by remember { mutableStateOf("") }
    var editLocation by remember { mutableStateOf("") }
    var editBio by remember { mutableStateOf("") }
    var editPassword by remember { mutableStateOf("") }
    var actionMsg by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    fun loadUsers() {
        scope.launch {
            loading = true
            try {
                val token = tokenManager.accessToken ?: return@launch
                val res = RetrofitInstance.adminApi.getUsers("Bearer $token")
                users = res.users
            } catch (_: Exception) {
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(Unit) { loadUsers() }

    if (selected != null) {
        val u = selected!!
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            IconButton(onClick = { selected = null; actionMsg = null }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
            }
            Text(u.name, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(u.email, color = Color.Gray)
            Text("Role: ${u.role}", fontSize = 14.sp)
            detailNote?.let { Text(it, fontSize = 12.sp, color = Color.Gray) }
            Spacer(modifier = Modifier.height(12.dp))

            Text("Edit account", fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = editName, onValueChange = { editName = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = editEmail, onValueChange = { editEmail = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = editPhone, onValueChange = { editPhone = it }, label = { Text("Phone") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = editLocation, onValueChange = { editLocation = it }, label = { Text("Location") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = editBio, onValueChange = { editBio = it }, label = { Text("Bio") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = editPassword,
                onValueChange = { editPassword = it },
                label = { Text("New password (optional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = {
                    scope.launch {
                        busy = true
                        actionMsg = null
                        try {
                            val token = tokenManager.accessToken ?: return@launch
                            val res = RetrofitInstance.adminApi.updateUser(
                                "Bearer $token",
                                u.userId,
                                com.example.travira.remote.AdminUpdateUserRequest(
                                    name = editName.trim().ifBlank { null },
                                    email = editEmail.trim().ifBlank { null },
                                    phone = editPhone.trim().ifBlank { null },
                                    location = editLocation.trim().ifBlank { null },
                                    bio = editBio.trim().ifBlank { null },
                                    password = editPassword.trim().ifBlank { null }
                                )
                            )
                            selected = res.user ?: selected
                            editPassword = ""
                            actionMsg = res.message ?: "User updated"
                            loadUsers()
                        } catch (e: Exception) {
                            actionMsg = e.message
                        } finally {
                            busy = false
                        }
                    }
                },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (busy) "Saving…" else "Save changes") }

            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    scope.launch {
                        busy = true
                        try {
                            val token = tokenManager.accessToken ?: return@launch
                            RetrofitInstance.adminApi.deleteUser("Bearer $token", u.userId)
                            selected = null
                            loadUsers()
                        } catch (e: Exception) {
                            actionMsg = e.message
                        } finally {
                            busy = false
                        }
                    }
                },
                enabled = !busy,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Delete user") }

            actionMsg?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(it, color = Color(0xFF1565C0), fontSize = 13.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("Wishlist (${u.wishlist.size})", fontWeight = FontWeight.Medium)
            u.wishlist.forEach { Text("• ${it.name}", fontSize = 13.sp) }
            Spacer(modifier = Modifier.height(8.dp))
            Text("Visited (${u.visitedPlaces.size})", fontWeight = FontWeight.Medium)
            u.visitedPlaces.forEach { v ->
                Text("• ${v.place?.name ?: "—"}", fontSize = 13.sp)
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
        return
    }

    if (loading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
    } else {
        LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                Text("${users.size} users", fontWeight = FontWeight.Medium, color = Color.Gray)
            }
            items(users, key = { it.userId }) { user ->
                Card(
                    onClick = {
                        selected = user
                        editName = user.name
                        editEmail = user.email
                        editPhone = user.phone.orEmpty()
                        editLocation = user.location.orEmpty()
                        editBio = user.bio.orEmpty()
                        editPassword = ""
                        actionMsg = null
                        scope.launch {
                            try {
                                val token = tokenManager.accessToken ?: return@launch
                                val res = RetrofitInstance.adminApi.getUserDetail("Bearer $token", user.userId)
                                selected = res.user ?: user
                                detailNote = res.passwordNote
                                res.user?.let {
                                    editName = it.name
                                    editEmail = it.email
                                    editPhone = it.phone.orEmpty()
                                    editLocation = it.location.orEmpty()
                                    editBio = it.bio.orEmpty()
                                }
                            } catch (_: Exception) { }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(user.name, fontWeight = FontWeight.Bold)
                        Text(user.email, fontSize = 13.sp, color = Color.Gray)
                        Text("Role: ${user.role}", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
