package com.example.travira.screens.auth

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.travira.remote.ApiErrorHelper
import com.example.travira.remote.ForgotPasswordRequest
import com.example.travira.remote.ResetPasswordRequest
import com.example.travira.remote.RetrofitInstance
import kotlinx.coroutines.launch

/**
 * Step 1: request reset email.
 * Step 2 (optional in-app): paste token from email/dev + set new password.
 * Primary path is the web page linked in the email.
 */
@Composable
fun ForgotPasswordScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var step by remember { mutableStateOf(0) } // 0 = email, 1 = token+password
    var email by remember { mutableStateOf("") }
    var token by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var success by remember { mutableStateOf<String?>(null) }
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
        focusedContainerColor = Color.White.copy(alpha = 0.55f),
        unfocusedContainerColor = Color.White.copy(alpha = 0.35f)
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0B1D2A), Color(0xFF0D47A1), Color(0xFF1565C0))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(top = 72.dp, bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Forgot password",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(Modifier.height(8.dp))
            Text(
                if (step == 0)
                    "We’ll email you a secure link to set a new password."
                else
                    "Prefer the in-app form? Paste the token from the email link and set a new password.",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.88f),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(28.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color.White.copy(alpha = 0.18f))
                    .border(
                        BorderStroke(1.2.dp, Color.White.copy(alpha = 0.45f)),
                        RoundedCornerShape(28.dp)
                    )
                    .padding(20.dp)
            ) {
                if (step == 0) {
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
                } else {
                    OutlinedTextField(
                        value = token,
                        onValueChange = { token = it },
                        label = { Text("Reset token") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = fieldColors
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("New password") },
                        leadingIcon = { Icon(Icons.Default.Lock, null) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = fieldColors
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text("Confirm password") },
                        leadingIcon = { Icon(Icons.Default.Lock, null) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = fieldColors
                    )
                }

                if (error != null) {
                    Spacer(Modifier.height(12.dp))
                    Text(error!!, color = Color(0xFFFFCDD2), fontSize = 13.sp)
                }
                if (success != null) {
                    Spacer(Modifier.height(12.dp))
                    Text(success!!, color = Color(0xFFC8E6C9), fontSize = 13.sp)
                }
            }

            Spacer(Modifier.height(22.dp))

            Button(
                onClick = {
                    if (loading) return@Button
                    error = null
                    success = null
                    if (step == 0) {
                        if (email.isBlank()) {
                            error = "Enter your account email"
                            return@Button
                        }
                        loading = true
                        scope.launch {
                            try {
                                val res = RetrofitInstance.authApi.forgotPassword(
                                    ForgotPasswordRequest(email.trim())
                                )
                                // Auto-fill token when server returns it (SMTP not configured)
                                res.resetToken?.takeIf { it.isNotBlank() }?.let { token = it }
                                success = buildString {
                                    append(
                                        res.message
                                            ?: "If that email is registered, a reset link was sent."
                                    )
                                    if (!res.resetToken.isNullOrBlank()) {
                                        append("\n\nToken filled below — enter new password & confirm.")
                                    }
                                    if (!res.resetLink.isNullOrBlank()) {
                                        append("\n\nOr open in browser:\n")
                                        append(res.resetLink)
                                    }
                                }
                                step = 1
                            } catch (e: Exception) {
                                error = ApiErrorHelper.message(e)
                            } finally {
                                loading = false
                            }
                        }
                    } else {
                        if (token.isBlank() || password.isBlank()) {
                            error = "Token and new password are required"
                            return@Button
                        }
                        if (password != confirmPassword) {
                            error = "Passwords do not match"
                            return@Button
                        }
                        if (password.length < 6) {
                            error = "Password must be at least 6 characters"
                            return@Button
                        }
                        loading = true
                        scope.launch {
                            try {
                                val resetBody = ResetPasswordRequest(
                                    token = token.trim(),
                                    password = password,
                                    confirmPassword = confirmPassword
                                )
                                val res = try {
                                    RetrofitInstance.authApi.resetPassword(resetBody)
                                } catch (first: Exception) {
                                    try {
                                        RetrofitInstance.authApi.resetPasswordAlt(resetBody)
                                    } catch (_: Exception) {
                                        throw first
                                    }
                                }
                                success = res.message ?: "Password updated. You can log in now."
                            } catch (e: Exception) {
                                error = ApiErrorHelper.message(e)
                            } finally {
                                loading = false
                            }
                        }
                    }
                },
                enabled = !loading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color(0xFF0B1D2A)
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
                        if (step == 0) "Send reset link" else "Update password",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }

            if (step == 1) {
                TextButton(onClick = {
                    step = 0
                    error = null
                    success = null
                }) {
                    Text("Back to email step", color = Color.White)
                }
            }

            Text(
                "Open the link in your email to change the password on the secure web form. " +
                    "You can also paste the token here if needed.",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 12.dp)
            )
        }

        IconButton(
            onClick = onBack,
            modifier = Modifier
                .padding(12.dp)
                .align(Alignment.TopStart)
                .zIndex(1f)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.15f))
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
        }
    }
}
