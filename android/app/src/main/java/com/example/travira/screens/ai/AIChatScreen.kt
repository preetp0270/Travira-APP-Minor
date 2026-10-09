package com.example.travira.screens.ai

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.shadow
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
private val CanvasIce = Color(0xFFF0F6FC)
private val TextPrimary = Color(0xFF0D1B2A)
private val TextSecondary = Color(0xFF546E7A)

private data class UiMessage(
    val id: String,
    val text: String,
    val isUser: Boolean,
    val isError: Boolean = false
)

private val SuggestedPrompts = listOf(
    "Plan 3 days in Goa",
    "Best street food in Kyoto",
    "Budget itinerary for Amalfi",
    "Cultural tips for Japan"
)

@Composable
fun AIChatScreen(
    tokenManager: TokenManager,
    onRequireLogin: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val welcome = UiMessage(
        id = "welcome",
        text = "Hello! I’m Travira AI — your Gemini travel guide. Ask about destinations, itineraries, culture, food, or packing tips.",
        isUser = false
    )
    val messages = remember { mutableStateListOf(welcome) }
    var input by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val showPrompts = messages.size <= 1 && !sending

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    fun resetChat() {
        messages.clear()
        messages.add(welcome)
        input = ""
    }

    fun send(textOverride: String? = null) {
        val text = (textOverride ?: input).trim()
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
                    .dropLast(1)
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

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.3f
    val bg = if (isDark) MaterialTheme.colorScheme.background else CanvasIce

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(bg)
            .statusBarsPadding()
            .imePadding()
            .padding(bottom = 108.dp)
    ) {
        // Stitch header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(BrandBlue.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.SmartToy, contentDescription = null, tint = BrandBlue)
            }
            Spacer(Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Travira AI Assistant",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = if (isDark) Color.White else TextPrimary
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = BrandBlue,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(Modifier.size(4.dp))
                    Text(
                        "Gemini · Travel companion",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }
            IconButton(onClick = { resetChat() }) {
                Icon(
                    Icons.Default.RestartAlt,
                    contentDescription = "Reset",
                    tint = TextSecondary
                )
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                MessageBubble(msg, isDark)
            }

            if (showPrompts) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "Try asking:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextSecondary,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SuggestedPrompts.forEach { prompt ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(Color.White)
                                        .border(1.dp, Color(0xFFBAE6FD), RoundedCornerShape(20.dp))
                                        .clickable { send(prompt) }
                                        .padding(horizontal = 14.dp, vertical = 10.dp)
                                ) {
                                    Text(
                                        prompt,
                                        fontSize = 13.sp,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (sending) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 4.dp, bottomEnd = 18.dp))
                                .background(Color.White)
                                .shadow(2.dp, RoundedCornerShape(18.dp))
                                .padding(horizontal = 16.dp, vertical = 14.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = BrandBlue
                            )
                        }
                    }
                }
            }
        }

        // Input bar — pill style (Stitch)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp),
                placeholder = {
                    Text("Ask Travira AI…", color = TextSecondary, fontSize = 14.sp)
                },
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BrandBlue,
                    unfocusedBorderColor = Color(0xFFCFD8DC),
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    cursorColor = BrandBlue
                ),
                maxLines = 4,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { send() }),
                enabled = !sending
            )
            Spacer(Modifier.size(8.dp))
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(BrandBlue)
                    .clickable(enabled = !sending && input.isNotBlank()) { send() },
                contentAlignment = Alignment.Center
            ) {
                if (sending) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                } else {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(msg: UiMessage, isDark: Boolean) {
    val isUser = msg.isUser
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier
                    .padding(end = 8.dp, top = 4.dp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(BrandBlue.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.SmartToy,
                    contentDescription = null,
                    tint = BrandBlue,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        Box(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 18.dp,
                        topEnd = 18.dp,
                        bottomStart = if (isUser) 18.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 18.dp
                    )
                )
                .background(
                    when {
                        msg.isError -> Color(0xFFFFEBEE)
                        isUser -> BrandBlue
                        else -> Color.White
                    }
                )
                .then(
                    if (!isUser && !msg.isError) {
                        Modifier.border(1.dp, Color(0xFFE3F2FD), RoundedCornerShape(18.dp))
                    } else Modifier
                )
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Text(
                text = msg.text,
                color = when {
                    msg.isError -> Color(0xFFC62828)
                    isUser -> Color.White
                    else -> TextPrimary
                },
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
        }
    }
}

private fun Color.luminance(): Float {
    val r = red
    val g = green
    val b = blue
    return 0.2126f * r + 0.7152f * g + 0.0722f * b
}
