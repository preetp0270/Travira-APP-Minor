package com.example.travira.screens.auth

import android.content.*
import android.net.*
import android.util.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.*
import androidx.compose.material.icons.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.tooling.preview.*
import androidx.compose.ui.unit.*
import com.example.travira.*
import com.example.travira.R
import com.example.travira.auth.*
import com.example.travira.remote.*
import com.example.travira.ui.theme.*
import kotlinx.coroutines.*

private const val ADMIN_REGISTER_URL =
    "https://travira-app-minor.onrender.com/admin-register.html"

@Composable
fun LoginScreen(
    tokenManager: TokenManager,
    onLoginSuccess: () -> Unit,
    onBack: () -> Unit,
    onForgotPassword: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isRegister by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var showConfirmPassword by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Color(0xFF0D1B2A),
        unfocusedTextColor = Color(0xFF0D1B2A),
        focusedLabelColor = Color(0xFF1565C0),
        unfocusedLabelColor = Color(0xFF78909C),
        cursorColor = Color(0xFF1565C0),
        focusedBorderColor = Color(0xFF1565C0),
        unfocusedBorderColor = Color(0xFFB0BEC5).copy(alpha = 0.7f),
        focusedLeadingIconColor = Color(0xFF1565C0),
        unfocusedLeadingIconColor = Color(0xFF90A4AE),
        focusedTrailingIconColor = Color(0xFF1565C0),
        unfocusedTrailingIconColor = Color(0xFF90A4AE),
        focusedContainerColor = Color.White.copy(alpha = 0.55f),
        unfocusedContainerColor = Color.White.copy(alpha = 0.35f)
    )

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // World wonders wallpaper background (login + register)
        Image(
            painter = painterResource(R.drawable.background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        // Soft light scrim so form content stays readable over the illustration
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White.copy(alpha = 0.22f))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(top = 72.dp, bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .shadow(12.dp, CircleShape)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(Color.White.copy(alpha = 0.95f), Color(0xFFE3F2FD))
                        )
                    )
                    .border(1.5.dp, Color.White.copy(alpha = 0.9f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.TravelExplore,
                    contentDescription = null,
                    tint = Color(0xFF1565C0),
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Travira",
                fontFamily = TraviraBrandFont,
                fontSize = 42.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0xFF0D47A1),
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (isRegister) "Create your account" else "Welcome back, explorer",
                fontSize = 15.sp,
                color = Color(0xFF37474F)
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Glass card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(16.dp, RoundedCornerShape(28.dp))
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color.White.copy(alpha = 0.82f))
                    .border(
                        BorderStroke(1.2.dp, Color.White.copy(alpha = 0.9f)),
                        RoundedCornerShape(28.dp)
                    )
                    .padding(20.dp)
            ) {
                if (isRegister) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Name") },
                        leadingIcon = { Icon(Icons.Default.Person, null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = fieldColors
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    leadingIcon = { Icon(Icons.Default.Email, null) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = fieldColors
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    leadingIcon = { Icon(Icons.Default.Lock, null) },
                    singleLine = true,
                    visualTransformation = if (showPassword) VisualTransformation.None
                    else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                if (showPassword) Icons.Default.VisibilityOff
                                else Icons.Default.Visibility,
                                contentDescription = null
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = fieldColors
                )

                if (isRegister) {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text("Confirm password") },
                        leadingIcon = { Icon(Icons.Default.Lock, null) },
                        singleLine = true,
                        visualTransformation = if (showConfirmPassword) VisualTransformation.None
                        else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showConfirmPassword = !showConfirmPassword }) {
                                Icon(
                                    if (showConfirmPassword) Icons.Default.VisibilityOff
                                    else Icons.Default.Visibility,
                                    contentDescription = null
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = fieldColors
                    )
                }

                if (error != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = error!!,
                        color = Color(0xFFB71C1C),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFFFEBEE))
                            .padding(10.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            Button(
                onClick = {
                    if (email.isBlank() || password.isBlank() || (isRegister && name.isBlank())) {
                        error = "Please fill all fields"
                        return@Button
                    }
                    if (isRegister) {
                        if (confirmPassword.isBlank()) {
                            error = "Please confirm your password"
                            return@Button
                        }
                        if (password != confirmPassword) {
                            error = "Password and confirm password do not match"
                            return@Button
                        }
                        if (password.length < 6) {
                            error = "Password must be at least 6 characters"
                            return@Button
                        }
                    }
                    if (loading) return@Button
                    loading = true
                    error = null
                    scope.launch {
                        try {
                            if (isRegister) {
                                RetrofitInstance.authApi.register(
                                    RegisterRequest(name.trim(), email.trim(), password)
                                )
                                val login = RetrofitInstance.authApi.login(
                                    LoginRequest(email.trim(), password)
                                )
                                val u = login.user
                                tokenManager.saveSession(
                                    accessToken = login.accessToken,
                                    refreshToken = login.refreshToken,
                                    userId = u?.id ?: u?._id ?: "",
                                    name = u?.name ?: name.trim(),
                                    email = u?.email ?: email.trim(),
                                    role = u?.role ?: "user"
                                )
                            } else {
                                val login = RetrofitInstance.authApi.login(
                                    LoginRequest(email.trim(), password)
                                )
                                val u = login.user
                                tokenManager.saveSession(
                                    accessToken = login.accessToken,
                                    refreshToken = login.refreshToken,
                                    userId = u?.id ?: u?._id ?: "",
                                    name = u?.name ?: "",
                                    email = u?.email ?: email.trim(),
                                    role = u?.role ?: "user"
                                )
                            }
                            onLoginSuccess()
                        } catch (e: Exception) {
                            error = ApiErrorHelper.message(e)
                            Log.e("TRAVIRA_AUTH", "Login/Register failed: ${e.message}", e)
                        } finally {
                            loading = false
                        }
                    }
                },
                enabled = !loading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .shadow(10.dp, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color(0xFF0B1D2A),
                    disabledContainerColor = Color.White.copy(alpha = 0.7f)
                )
            ) {
                if (loading) {
                    CircularProgressIndicator(
                        color = Color(0xFF1565C0),
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.5.dp
                    )
                } else {
                    Text(
                        text = if (isRegister) "Create account" else "Sign in",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (!isRegister) {
                TextButton(onClick = onForgotPassword) {
                    Text(
                        text = "Forgot password?",
                        color = Color(0xFF1565C0),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            TextButton(onClick = {
                isRegister = !isRegister
                error = null
                confirmPassword = ""
            }) {
                Text(
                    text = if (isRegister) "Already have an account? Sign in"
                    else "New here? Create account",
                    color = Color(0xFF1565C0),
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Browse as a guest anytime.\nLogin unlocks wishlist, visited places & AI chat.",
                fontSize = 12.sp,
                color = Color(0xFF546E7A),
                textAlign = TextAlign.Center,
                lineHeight = 17.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Admin registration — opens in the system browser
            val context = LocalContext.current
            TextButton(
                onClick = {
                    try {
                        val intent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse(ADMIN_REGISTER_URL)
                        )
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Log.e("TRAVIRA_AUTH", "Could not open admin register URL", e)
                    }
                }
            ) {
                Text(
                    text = "Admin? Register here",
                    color = Color(0xFF0D47A1),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    textDecoration = TextDecoration.Underline,
                    textAlign = TextAlign.Center
                )
            }
        }

        IconButton(
            onClick = {
                Log.d("TRAVIRA_AUTH", "LoginScreen UI back button clicked")
                onBack()
            },
            modifier = Modifier
                .padding(top = 12.dp, start = 8.dp)
                .align(Alignment.TopStart)
                .zIndex(2f)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.85f))
                .border(1.dp, Color(0xFFB0BEC5).copy(alpha = 0.5f), CircleShape)
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color(0xFF0D47A1)
            )
        }
    }
}

@Preview(showBackground = true, name = "Login", showSystemUi = true)
@Composable
private fun LoginScreenPreview() {
    val context = androidx.compose.ui.platform.LocalContext.current
    TraviraTheme {
        LoginScreen(
            tokenManager = com.example.travira.auth.TokenManager(context),
            onLoginSuccess = {},
            onBack = {},
            onForgotPassword = {}
        )
    }
}

