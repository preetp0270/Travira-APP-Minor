package com.example.travira.screens.splash

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.*
import androidx.compose.ui.res.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.tooling.preview.*
import androidx.compose.ui.unit.*
import com.example.travira.R
import com.example.travira.ui.theme.*
import kotlinx.coroutines.*

/**
 * Simple branded splash (no ExoPlayer required).
 * Replace with video splash later when media3 + raw/travira_splash.mp4 are set up.
 */
@Composable
fun SplashScreen(
    onFinish: () -> Unit
) {
    LaunchedEffect(Unit) {
        delay(0)
        onFinish()
    }
}


@Preview(showBackground = true, name = "Splash", showSystemUi = true)
@Composable
private fun SplashScreenPreview() {
    TraviraTheme {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
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
            Text(
                text = "Travira",
                fontFamily = TraviraBrandFont,
                color = Color(0xFF0D47A1),
                fontSize = 48.sp,
                fontWeight = FontWeight.Normal
            )
        }
    }
}

