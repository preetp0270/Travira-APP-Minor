package com.example.travira.screens.splash

import androidx.compose.ui.tooling.preview.Preview
import com.example.travira.ui.theme.TraviraTheme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

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

//    Box(
//        modifier = Modifier
//            .fillMaxSize()
//            .background(
//                Brush.verticalGradient(
//                    listOf(Color(0xFF0B1D2A), Color(0xFF1565C0))
//                )
//            ),
//        contentAlignment = Alignment.Center
//    ) {
//        Column(horizontalAlignment = Alignment.CenterHorizontally) {
//            Text(
//                text = "Travira",
//                color = Color.White,
//                fontSize = 40.sp,
//                fontWeight = FontWeight.Bold
//            )
//            Spacer(modifier = Modifier.height(8.dp))
//            Text(
//                text = "Explore smarter. Discover deeper.",
//                color = Color.White.copy(alpha = 0.85f),
//                fontSize = 14.sp
//            )
//            Spacer(modifier = Modifier.height(28.dp))
//            CircularProgressIndicator(color = Color.White)
//        }
//    }
}


@Preview(showBackground = true, name = "Splash", showSystemUi = true)
@Composable
private fun SplashScreenPreview() {
    TraviraTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF0B1D2A), Color(0xFF1565C0))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Travira",
                color = Color.White,
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

