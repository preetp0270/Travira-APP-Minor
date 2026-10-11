package com.example.travira.screens.splash

import android.net.*
import android.view.*
import android.widget.*
import androidx.annotation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.tooling.preview.*
import androidx.compose.ui.unit.*
import androidx.compose.ui.viewinterop.*
import androidx.media3.common.*
import androidx.media3.common.util.*
import androidx.media3.exoplayer.*
import androidx.media3.ui.*
import com.example.travira.*
import com.example.travira.ui.theme.*

/**
 * Full-screen intro video after the logo splash.
 *
 * Put your file at:
 *   android/app/src/main/res/raw/travira_intro.mp4
 *
 * If the file is missing, [onFinish] is called immediately.
 */
@OptIn(UnstableApi::class)
@Composable
fun IntroVideoScreen(
    onFinish: () -> Unit
) {
    val context = LocalContext.current

    val videoResId = remember {
        context.resources.getIdentifier("travira_intro", "raw", context.packageName)
    }

    // No video asset yet → skip straight to the app
    if (videoResId == 0) {
        androidx.compose.runtime.LaunchedEffect(Unit) { onFinish() }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        )
        return
    }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            val uri = Uri.parse("android.resource://${context.packageName}/$videoResId")
            setMediaItem(MediaItem.fromUri(uri))
            repeatMode = Player.REPEAT_MODE_OFF
            playWhenReady = true
            prepare()
        }
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    onFinish()
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        )

//        // Skip control
//        Text(
//            text = "Skip",
//            color = Color.White,
//            fontSize = 14.sp,
//            fontWeight = FontWeight.SemiBold,
//            modifier = Modifier
//                .align(Alignment.TopEnd)
//                .padding(top = 48.dp, end = 20.dp)
//                .clip(RoundedCornerShape(20.dp))
//                .background(Color.Black.copy(alpha = 0.45f))
//                .clickable {
//                    exoPlayer.stop()
//                    onFinish()
//                }
//                .padding(horizontal = 16.dp, vertical = 8.dp)
//        )
    }
}


@Preview(showBackground = true, name = "Intro Video", showSystemUi = true)
@Composable
private fun IntroVideoScreenPreview() {
    TraviraTheme {
        // Preview shows black placeholder (video asset may be missing in design view)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        )
    }
}

