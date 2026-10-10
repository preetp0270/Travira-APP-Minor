package com.example.travira.screens.auth

import androidx.compose.ui.tooling.preview.Preview
import com.example.travira.ui.theme.TraviraTheme
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.travira.auth.TokenManager
import com.example.travira.remote.ApiErrorHelper
import com.example.travira.remote.LoginRequest
import com.example.travira.remote.RegisterRequest
import com.example.travira.remote.RetrofitInstance
import kotlinx.coroutines.launch

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
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF0B1D2A),
                        Color(0xFF0D47A1),
                        Color(0xFF1565C0),
                        Color(0xFF42A5F5)
                    )
                )
            )
    ) {
        // Soft glow orbs for depth (use offset — negative padding can crash some Compose builds)
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 40.dp)
                .size(180.dp)
                .clip(CircleShape)
                .background(Color(0xFF90CAF9).copy(alpha = 0.25f))
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(bottom = 80.dp)
                .size(220.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.12f))
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
                    .border(1.5.dp, Color.White.copy(alpha = 0.8f), CircleShape),
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
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (isRegister) "Create your account" else "Welcome back, explorer",
                fontSize = 15.sp,
                color = Color.White.copy(alpha = 0.88f)
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Glass card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(16.dp, RoundedCornerShape(28.dp))
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color.White.copy(alpha = 0.18f))
                    .border(
                        BorderStroke(1.2.dp, Color.White.copy(alpha = 0.45f)),
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
                        color = Color(0xFFFFCDD2),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFB71C1C).copy(alpha = 0.45f))
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
                        color = Color.White.copy(alpha = 0.95f),
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
                    color = Color.White.copy(alpha = 0.95f),
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Browse as a guest anytime.\nLogin unlocks wishlist, visited places & AI chat.",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.7f),
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
                    color = Color.White.copy(alpha = 0.95f),
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
                .background(Color.White.copy(alpha = 0.15f))
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.White
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

