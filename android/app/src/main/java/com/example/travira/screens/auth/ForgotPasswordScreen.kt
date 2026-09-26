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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.travira.remote.ApiErrorHelper
import com.example.travira.remote.ForgotPasswordRequest
import com.example.travira.remote.RetrofitInstance
import kotlinx.coroutines.launch

/**
 * Request a password-reset email. User opens the secure web form from the link
 * (no in-app token/password step). After reset, all other sessions are invalidated.
 */
@Composable
fun ForgotPasswordScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var email by remember { mutableStateOf("") }
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
                "We’ll email you a secure link. Open it in your browser to set a new password.",
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
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    leadingIcon = { Icon(Icons.Default.Email, null) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = fieldColors,
                    enabled = success == null
                )

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

            if (success == null) {
                Button(
                    onClick = {
                        if (loading) return@Button
                        error = null
                        if (email.isBlank()) {
                            error = "Enter your account email"
                            return@Button
                        }
                        loading = true
                        scope.launch {
                            try {
                                val reqBody = ForgotPasswordRequest(email.trim())
                                val res = try {
                                    RetrofitInstance.authApi.forgotPassword(reqBody)
                                } catch (first: Exception) {
                                    try {
                                        RetrofitInstance.authApi.forgotPasswordAlt(reqBody)
                                    } catch (_: Exception) {
                                        throw first
                                    }
                                }
                                // Never show tokens or reset URLs in the app (security).
                                success =
                                    "If that email is registered, a secure reset link was sent.\n\n" +
                                        "Open the link from your email (check spam). " +
                                        "After you change the password, you will be signed out on all devices."
                            } catch (e: Exception) {
                                error = ApiErrorHelper.message(e)
                            } finally {
                                loading = false
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
                            "Send reset link",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            } else {
                Button(
                    onClick = onBack,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color(0xFF0B1D2A)
                    )
                ) {
                    Text("Back to login", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }

            Text(
                "The email link opens a secure web form. Password change signs you out everywhere for safety.",
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
