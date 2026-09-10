package com.example.travira.screens.auth

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.travira.auth.TokenManager
import com.example.travira.remote.LoginRequest
import com.example.travira.remote.RegisterRequest
import com.example.travira.remote.RetrofitInstance
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    tokenManager: TokenManager,
    onLoginSuccess: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isRegister by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0B1D2A), Color(0xFF1565C0))
                )
            )
    ) {
        // Content first so the back button (drawn after) sits above it for hit-testing
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp)
                .padding(top = 80.dp, bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Travira",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (isRegister) "Create your account" else "Welcome back",
                fontSize = 15.sp,
                color = Color.White.copy(alpha = 0.85f)
            )

            Spacer(modifier = Modifier.height(36.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                tonalElevation = 4.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {

                    if (isRegister) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Name") },
                            leadingIcon = { Icon(Icons.Default.Person, null) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color(0xFF1A1A1A),
                                unfocusedTextColor = Color(0xFF1A1A1A),
                                focusedLabelColor = Color(0xFF1565C0),
                                unfocusedLabelColor = Color(0xFF757575),
                                cursorColor = Color(0xFF1565C0),
                                focusedBorderColor = Color(0xFF1565C0),
                                unfocusedBorderColor = Color(0xFFBDBDBD),
                                focusedLeadingIconColor = Color(0xFF1565C0),
                                unfocusedLeadingIconColor = Color(0xFF757575)
                            )
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
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF1A1A1A),
                            unfocusedTextColor = Color(0xFF1A1A1A),
                            focusedLabelColor = Color(0xFF1565C0),
                            unfocusedLabelColor = Color(0xFF757575),
                            cursorColor = Color(0xFF1565C0),
                            focusedBorderColor = Color(0xFF1565C0),
                            unfocusedBorderColor = Color(0xFFBDBDBD),
                            focusedLeadingIconColor = Color(0xFF1565C0),
                            unfocusedLeadingIconColor = Color(0xFF757575)
                        )
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
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF1A1A1A),
                            unfocusedTextColor = Color(0xFF1A1A1A),
                            focusedLabelColor = Color(0xFF1565C0),
                            unfocusedLabelColor = Color(0xFF757575),
                            cursorColor = Color(0xFF1565C0),
                            focusedBorderColor = Color(0xFF1565C0),
                            unfocusedBorderColor = Color(0xFFBDBDBD),
                            focusedLeadingIconColor = Color(0xFF1565C0),
                            unfocusedLeadingIconColor = Color(0xFF757575)
                        )
                    )

                    if (error != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = error!!,
                            color = Color(0xFFC62828),
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (email.isBlank() || password.isBlank() || (isRegister && name.isBlank())) {
                        error = "Please fill all fields"
                        return@Button
                    }
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
                            error = e.message ?: "Something went wrong"
                            Log.e("TRAVIRA_AUTH", "Login/Register failed: ${e.message}", e)
                        } finally {
                            loading = false
                        }
                    }
                },
                enabled = !loading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4FC3F7))
            ) {
                if (loading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.height(24.dp)
                    )
                } else {
                    Text(
                        text = if (isRegister) "Sign Up" else "Login",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF0B1D2A)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            TextButton(onClick = {
                isRegister = !isRegister
                error = null
            }) {
                Text(
                    text = if (isRegister) "Already have an account? Login"
                    else "New here? Create account",
                    color = Color.White.copy(alpha = 0.9f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "You can browse places as a guest.\nLogin is required for wishlist & AI chat.",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.65f),
                textAlign = TextAlign.Center,
                lineHeight = 16.sp
            )
        }

        // Drawn last + zIndex so it receives touches above the full-size Column
        IconButton(
            onClick = {
                Log.d("TRAVIRA_AUTH", "LoginScreen UI back button clicked")
                onBack()
            },
            modifier = Modifier
                .padding(12.dp)
                .align(Alignment.TopStart)
                .zIndex(1f)
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.White
            )
        }
    }
}
