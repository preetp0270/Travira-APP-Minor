package com.example.travira.screens.ai

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.travira.auth.TokenManager
import com.example.travira.remote.ApiErrorHelper
import com.example.travira.remote.ChatHistoryTurn
import com.example.travira.remote.ChatRequest
import com.example.travira.remote.RetrofitInstance
import kotlinx.coroutines.launch

private val BrandBlue = Color(0xFF1565C0)
private val SoftBg = Color(0xFFF0F6FC)

private data class UiMessage(
    val id: String,
    val text: String,
    val isUser: Boolean,
    val isError: Boolean = false
)

@Composable
fun AIChatScreen(
    tokenManager: TokenManager,
    onRequireLogin: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val messages = remember {
        mutableStateListOf(
            UiMessage(
                id = "welcome",
                text = "Hi! I’m Travira AI — your travel companion. Ask about destinations, itineraries, packing, visas, food, or safety tips.",
                isUser = false
            )
        )
    }
    var input by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    fun send() {
        val text = input.trim()
        if (text.isEmpty() || sending) return

        val token = tokenManager.accessToken
        if (token.isNullOrBlank() || !tokenManager.isLoggedIn) {
            onRequireLogin()
            return
        }

        messages.add(
            UiMessage(
                id = "u-${System.currentTimeMillis()}",
                text = text,
                isUser = true
            )
        )
        input = ""
        sending = true

        scope.launch {
            try {
                val history = messages
                    .filter { it.id != "welcome" && !it.isError }
                    .dropLast(1) // exclude the message we just added from history duplication; include prior turns
                    .takeLast(12)
                    .map {
                        ChatHistoryTurn(
                            role = if (it.isUser) "user" else "model",
                            text = it.text
                        )
                    }

                val res = RetrofitInstance.chatApi.chat(
                    bearer = "Bearer $token",
                    body = ChatRequest(message = text, history = history)
                )
                val reply = res.reply?.takeIf { it.isNotBlank() }
                    ?: res.message?.takeIf { it.isNotBlank() }
                    ?: "Sorry, I couldn’t answer that. Try another travel question."
                val failed = !res.success && res.reply.isNullOrBlank()
                messages.add(
                    UiMessage(
                        id = "a-${System.currentTimeMillis()}",
                        text = if (failed && reply.contains("GEMINI", ignoreCase = true)) {
                            "Chatbot is not configured on the server. Set GEMINI_API_KEY in Render environment variables."
                        } else reply,
                        isUser = false,
                        isError = failed
                    )
                )
            } catch (e: Exception) {
                messages.add(
                    UiMessage(
                        id = "e-${System.currentTimeMillis()}",
                        text = ApiErrorHelper.message(e),
                        isUser = false,
                        isError = true
                    )
                )
            } finally {
                sending = false
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SoftBg)
            .imePadding()
            // Keep content above system nav + Travira bottom bar (~80–100dp)
            .padding(bottom = 96.dp)
    ) {
        // Glass-style header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    androidx.compose.ui.graphics.Brush.horizontalGradient(
                        listOf(Color(0xFF0B1D2A), Color(0xFF1565C0), Color(0xFF1976D2))
                    )
                )
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.SmartToy,
                    contentDescription = null,
                    tint = Color.White
                )
            }
            Spacer(Modifier.size(12.dp))
            Column {
                Text(
                    "Travira AI",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Text(
                    "Travel-only assistant · Gemini",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 12.sp
                )
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 12.dp,
                bottom = 8.dp
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                MessageBubble(msg)
            }
            if (sending) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White)
                                .padding(horizontal = 14.dp, vertical = 12.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.dp,
                                color = BrandBlue
                            )
                        }
                    }
                }
            }
        }

        // Input bar — sits above the bottom navigation
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { if (it.length <= 2000) input = it },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp),
                placeholder = { Text("Ask about trips, places, tips…") },
                shape = RoundedCornerShape(24.dp),
                maxLines = 3,
                singleLine = false,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { send() }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BrandBlue,
                    unfocusedBorderColor = Color(0xFFCFD8DC),
                    cursorColor = BrandBlue,
                    focusedContainerColor = SoftBg,
                    unfocusedContainerColor = SoftBg
                )
            )
            Spacer(Modifier.size(8.dp))
            IconButton(
                onClick = { send() },
                enabled = !sending && input.isNotBlank(),
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (!sending && input.isNotBlank()) BrandBlue else Color(0xFFB0BEC5))
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
private fun MessageBubble(msg: UiMessage) {
    val isUser = msg.isUser
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 320.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isUser) 16.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 16.dp
                    )
                )
                .background(
                    when {
                        msg.isError -> Color(0xFFFFEBEE)
                        isUser -> BrandBlue
                        else -> Color.White
                    }
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(
                text = msg.text,
                color = when {
                    msg.isError -> Color(0xFFC62828)
                    isUser -> Color.White
                    else -> Color(0xFF212121)
                },
                fontSize = 15.sp,
                lineHeight = 21.sp
            )
        }
    }
}
