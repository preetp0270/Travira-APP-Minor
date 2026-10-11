package com.example.travira.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.*
import androidx.compose.ui.tooling.preview.*
import androidx.compose.ui.unit.*

data class BottomBarItem(
    val title: String,
    val icon: ImageVector
)

@Composable
fun TraviraBottomBar(
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        BottomBarItem("Home", Icons.Default.Home),
        BottomBarItem("AI Travel", Icons.Default.SmartToy),
        BottomBarItem("Profile", Icons.Default.Person)
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            // Extra gap below the floating bottom pill
            .padding(start = 60.dp, end = 60.dp, bottom = 28.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 12.dp,
                    shape = RoundedCornerShape(50.dp)
                ),
            shape = RoundedCornerShape(50.dp),
            color = Color(0xFF1565C0).copy(alpha = 0.28f),
            tonalElevation = 0.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Color.White.copy(alpha = 0.55f),
                        RoundedCornerShape(50.dp)
                    )
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEachIndexed { index, item ->
                    val selected = selectedIndex == index
                    val iconSize by animateDpAsState(
                        targetValue = if (selected) 30.dp else 25.dp,
                        label = ""
                    )
                    val brandBlue = Color(0xFF1565C0)
                    val iconColor by animateColorAsState(
                        targetValue = if (selected) brandBlue else Color(0xFF78909C),
                        label = ""
                    )

                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                color = if (selected)
                                    brandBlue.copy(alpha = 0.18f)
                                else
                                    Color.Transparent,
                                shape = CircleShape
                            )
                            .clickable { onItemSelected(index) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.title,
                            modifier = Modifier.size(iconSize),
                            tint = iconColor
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TraviraBottomBarPreview() {
    MaterialTheme {
        TraviraBottomBar(
            selectedIndex = 0,
            onItemSelected = {}
        )
    }
}
