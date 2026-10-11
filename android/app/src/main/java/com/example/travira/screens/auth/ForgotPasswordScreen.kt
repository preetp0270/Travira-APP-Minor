package com.example.travira.screens.auth

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
import androidx.compose.ui.res.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.tooling.preview.*
import androidx.compose.ui.unit.*
import com.example.travira.R
import com.example.travira.remote.*
import com.example.travira.ui.theme.*
import kotlinx.coroutines.*

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
        modifier = modifier.fillMaxSize()
    ) {
        Image(
            painter = painterResource(R.drawable.background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
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
            Text(
                text = "Travira",
                fontFamily = TraviraBrandFont,
                fontSize = 36.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0xFF0D47A1)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Forgot password",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0D1B2A)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "We’ll email you a secure link. Open it in your browser to set a new password.",
                fontSize = 14.sp,
                color = Color(0xFF546E7A),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(28.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color.White.copy(alpha = 0.82f))
                    .border(
                        BorderStroke(1.2.dp, Color.White.copy(alpha = 0.9f)),
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
                    Text(error!!, color = Color(0xFFC62828), fontSize = 13.sp)
                }
                if (success != null) {
                    Spacer(Modifier.height(12.dp))
                    Text(success!!, color = Color(0xFF2E7D32), fontSize = 13.sp)
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
                                try {
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
                color = Color(0xFF546E7A),
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
                .background(Color.White.copy(alpha = 0.85f))
                .border(1.dp, Color(0xFFB0BEC5).copy(alpha = 0.5f), CircleShape)
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color(0xFF0D47A1))
        }
    }
}


@Preview(showBackground = true, name = "Forgot Password", showSystemUi = true)
@Composable
private fun ForgotPasswordScreenPreview() {
    TraviraTheme {
        ForgotPasswordScreen(onBack = {})
    }
}

