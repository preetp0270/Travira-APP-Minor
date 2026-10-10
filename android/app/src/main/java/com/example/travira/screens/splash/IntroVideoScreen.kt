package com.example.travira.screens.splash

import androidx.compose.ui.tooling.preview.Preview
import com.example.travira.ui.theme.TraviraTheme

import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.travira.R

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

