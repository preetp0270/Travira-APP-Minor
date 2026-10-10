package com.example.travira.screens.profile

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.travira.auth.TokenManager
import com.example.travira.model.User
import com.example.travira.remote.RetrofitInstance
import com.example.travira.ui.theme.TraviraBlue
import com.example.travira.ui.theme.TraviraBlueDark
import com.example.travira.ui.theme.TraviraBlueLight
import com.example.travira.ui.theme.TraviraBg
import com.example.travira.ui.theme.TraviraError
import com.example.travira.ui.theme.TraviraHeart
import com.example.travira.ui.theme.TraviraHeartBg
import com.example.travira.ui.theme.TraviraIce
import com.example.travira.ui.theme.TraviraMutedIcon
import com.example.travira.ui.theme.TraviraOutline
import com.example.travira.ui.theme.TraviraSecondaryBright
import com.example.travira.ui.theme.TraviraSky
import com.example.travira.ui.theme.TraviraStar
import com.example.travira.ui.theme.TraviraStarBg
import com.example.travira.ui.theme.TraviraSuccess
import com.example.travira.ui.theme.TraviraSuccessBg
import com.example.travira.ui.theme.TraviraSurfaceLow
import com.example.travira.ui.theme.TraviraTextPrimary
import com.example.travira.ui.theme.TraviraTextSecondary
import com.example.travira.ui.theme.TraviraTextTertiary
import com.example.travira.ui.theme.TraviraWhite
import com.example.travira.ui.theme.TraviraBrandFont
import com.example.travira.ui.theme.TraviraTheme
import androidx.compose.ui.tooling.preview.Preview

enum class ProfileSection {
    WISHLIST, VISITED, REVIEWS
}

@Composable
fun ProfileScreen(
    isLoggedIn: Boolean,
    user: User?,
    tokenManager: TokenManager? = null,
    themeMode: String = "system",
    onThemeModeChange: (String) -> Unit = {},
    onLoginClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onSectionClick: (ProfileSection) -> Unit = {},
    onAdminClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val displayName = if (isLoggedIn) (user?.name?.ifBlank { "Traveler" } ?: "Traveler") else "Guest"
    val email = if (isLoggedIn) (user?.email.orEmpty()) else ""
    val bioFromUser = user?.bio
    val bio = when {
        !isLoggedIn -> "Browsing as guest. Login to save wishlist & use AI."
        !bioFromUser.isNullOrBlank() -> bioFromUser
        else -> "Explore smarter. Discover deeper. Travel with confidence."
    }
    val wishlistCount = user?.wishlist?.size ?: 0
    val visitedCount = user?.visitedPlaces?.size ?: 0
    // Count unique countries from visited places (simple number only — no extra list)
    val countriesCount = user?.visitedPlaces
        ?.mapNotNull { entry ->
            entry.place?.country?.trim()?.takeIf { it.isNotBlank() }
                ?: entry.place?.location?.substringAfterLast(",")?.trim()?.takeIf { it.isNotBlank() }
        }
        ?.distinct()
        ?.size
        ?: 0
    val userLocation = user?.location?.trim().orEmpty()
    val isAdmin = (user?.role?.equals("admin", ignoreCase = true) == true) ||
            (tokenManager?.isAdmin == true)

    val level = when {
        visitedCount >= 20 -> 5
        visitedCount >= 12 -> 4
        visitedCount >= 6 -> 3
        visitedCount >= 3 -> 2
        visitedCount >= 1 -> 1
        else -> 0
    }
    val nextMilestone = when {
        visitedCount < 5 -> 5
        visitedCount < 10 -> 10
        visitedCount < 20 -> 20
        else -> visitedCount + 5
    }
    val progress = (visitedCount.toFloat() / nextMilestone.toFloat()).coerceIn(0f, 1f)

    val scroll = rememberScrollState()
    val context = LocalContext.current

    var toastMsg by remember { mutableStateOf<String?>(null) }
    var reviewsCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(isLoggedIn, user?.userId) {
        if (!isLoggedIn) {
            reviewsCount = 0
            return@LaunchedEffect
        }
        val token = tokenManager?.accessToken ?: return@LaunchedEffect
        try {
            val res = RetrofitInstance.authApi.getMyRatings("Bearer $token")
            reviewsCount = res.count
        } catch (_: Exception) {
            reviewsCount = 0
        }
    }

    fun requireLoginThen(section: ProfileSection) {
        if (!isLoggedIn) onLoginClick() else onSectionClick(section)
    }

    fun cycleTheme() {
        val next = when (themeMode) {
            "light" -> "dark"
            "dark" -> "system"
            else -> "light"
        }
        onThemeModeChange(next)
        tokenManager?.themeMode = next
        toastMsg = "Appearance: ${next.replaceFirstChar { it.uppercase() }}"
    }

    val isDark = when (themeMode) {
        "dark" -> true
        "light" -> false
        else -> androidx.compose.foundation.isSystemInDarkTheme()
    }
    val bg = if (isDark) TraviraBlueDark else TraviraBg
    val cardBg = if (isDark) Color(0xFF102A43) else TraviraWhite
    val textPrimary = if (isDark) Color.White else TraviraTextPrimary
    val textSecondary = if (isDark) Color(0xFFB0BEC5) else TraviraTextSecondary
    val textTertiary = if (isDark) Color(0xFF78909C) else TraviraTextTertiary
    val surfaceLow = if (isDark) Color(0xFF1A3348) else TraviraSurfaceLow
    val outline = if (isDark) Color(0xFF2A4558) else TraviraOutline

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(scroll)
                .padding(bottom = 120.dp)
                .navigationBarsPadding()
        ) {

            // Travira brand title
            Text(
                text = "Travira",
                fontFamily = TraviraBrandFont,
                fontWeight = FontWeight.Bold,
                fontSize = 42.sp,
                color = TraviraBlue,
                textAlign = TextAlign.Center,
                maxLines = 1,
                softWrap = false,
                style = androidx.compose.ui.text.TextStyle(
                    platformStyle = androidx.compose.ui.text.PlatformTextStyle(
                        includeFontPadding = false
                    ),
                    lineHeight = 42.sp,
                    lineHeightStyle = androidx.compose.ui.text.style.LineHeightStyle(
                        alignment = androidx.compose.ui.text.style.LineHeightStyle.Alignment.Center,
                        trim = androidx.compose.ui.text.style.LineHeightStyle.Trim.Both
                    )
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 20.dp)

            )

            // Hero profile card — reduced gap below the title
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .offset(y = (-30).dp)
                    .shadow(
                        elevation = 8.dp,
                        shape = RoundedCornerShape(20.dp),
                        ambientColor = Color(0x331565C0),
                        spotColor = Color(0x221565C0)
                    )
                    .clip(RoundedCornerShape(20.dp))
                    .background(cardBg)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = 16.dp,
                            end = 16.dp,
                            top = 12.dp,
                            bottom = 12.dp
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Avatar with ring
                    Box(contentAlignment = Alignment.BottomEnd) {
                        Box(
                            modifier = Modifier
                                .size(68.dp)
                                .border(
                                    width = 2.5.dp,
                                    brush = Brush.linearGradient(
                                        listOf(
                                            TraviraBlue,
                                            TraviraSky,
                                            TraviraBlueLight
                                        )
                                    ),
                                    shape = CircleShape
                                )
                                .padding(3.dp)
                                .clip(CircleShape)
                                .background(TraviraIce),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                tint = TraviraBlue,
                                modifier = Modifier.size(34.dp)
                            )
                        }

                        if (isLoggedIn) {
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(TraviraBlue)
                                    .border(2.dp, cardBg, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Verified,
                                    contentDescription = "Verified",
                                    tint = Color.White,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    Text(
                        text = displayName,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = textPrimary
                    )

                    if (email.isNotBlank()) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = email,
                            fontSize = 12.sp,
                            color = textSecondary
                        )
                    }

                    val roleLabel = when {
                        !isLoggedIn -> "Guest"
                        isAdmin -> "Admin"
                        else -> "User"
                    }

                    Spacer(Modifier.height(4.dp))

                    if (userLocation.isNotBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Default.Place,
                                contentDescription = null,
                                tint = textTertiary,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = userLocation,
                                fontSize = 12.sp,
                                color = textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                "·",
                                fontSize = 12.sp,
                                color = textTertiary
                            )
                            Text(
                                text = roleLabel,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isAdmin) TraviraBlue else textSecondary
                            )
                        }
                    } else {
                        Text(
                            text = roleLabel,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isAdmin) TraviraBlue else textSecondary
                        )
                    }

                    Spacer(Modifier.height(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.Explore,
                            contentDescription = null,
                            tint = TraviraBlue,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = if (isLoggedIn) {
                                "Travel Explorer • Level $level"
                            } else {
                                "Guest explorer"
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = TraviraSecondaryBright
                        )
                    }

                    Spacer(Modifier.height(6.dp))

                    Text(
                        text = bio,
                        fontSize = 12.sp,
                        color = textSecondary,
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (!isLoggedIn) {
                        Spacer(Modifier.height(16.dp))

                        Button(
                            onClick = onLoginClick,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = TraviraBlue
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Text(
                                "Login / Sign up",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Stats bento grid
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatPill(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.CheckCircle,
                    iconTint = TraviraSuccess,
                    iconBg = if (isDark) Color(0xFF1B3A2A) else TraviraSuccessBg,
                    label = "Visited",
                    value = visitedCount.toString(),
                    cardBg = cardBg,
                    textPrimary = textPrimary,
                    textTertiary = textTertiary,
                    onClick = { requireLoginThen(ProfileSection.VISITED) }
                )
                StatPill(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Favorite,
                    iconTint = TraviraHeart,
                    iconBg = if (isDark) Color(0xFF3A1A28) else TraviraHeartBg,
                    label = "Wishlist",
                    value = wishlistCount.toString(),
                    cardBg = cardBg,
                    textPrimary = textPrimary,
                    textTertiary = textTertiary,
                    onClick = { requireLoginThen(ProfileSection.WISHLIST) }
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatPill(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Star,
                    iconTint = TraviraStar,
                    iconBg = if (isDark) Color(0xFF3A3010) else TraviraStarBg,
                    label = "Reviews",
                    value = reviewsCount.toString(),
                    cardBg = cardBg,
                    textPrimary = textPrimary,
                    textTertiary = textTertiary,
                    onClick = { requireLoginThen(ProfileSection.REVIEWS) }
                )
                StatPill(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Public,
                    iconTint = TraviraBlue,
                    iconBg = surfaceLow,
                    label = "Countries",
                    value = countriesCount.toString(),
                    cardBg = cardBg,
                    textPrimary = textPrimary,
                    textTertiary = textTertiary,
                    onClick = { requireLoginThen(ProfileSection.VISITED) }
                )
            }

            Spacer(Modifier.height(12.dp))

            // Passport milestones teaser
            if (isLoggedIn) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(cardBg)
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Passport Milestones",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            color = textPrimary
                        )
                        Text(
                            text = "Next: $nextMilestone places",
                            fontSize = 12.sp,
                            color = textTertiary
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    // Progress bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(50))
                            .background(surfaceLow)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progress)
                                .height(8.dp)
                                .clip(RoundedCornerShape(50))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(TraviraBlue, TraviraBlueLight)
                                    )
                                )
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "$visitedCount / $nextMilestone destinations logged",
                        fontSize = 12.sp,
                        color = textSecondary
                    )
                }
                Spacer(Modifier.height(12.dp))
            }

            // Travel Activity section
            SectionHeader(
                title = "Travel Activity",
                caption = "Journal",
                textPrimary = textPrimary,
                textTertiary = textTertiary
            )
            ProfileSectionCard(cardBg = cardBg, outline = outline) {
                ProfileRow(
                    icon = Icons.Default.Favorite,
                    iconTint = TraviraHeart,
                    iconBg = if (isDark) Color(0xFF3A1A28) else TraviraHeartBg,
                    title = "My Wishlist",
                    subtitle = if (wishlistCount > 0) "$wishlistCount saved places" else "Save places for later",
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    onClick = { requireLoginThen(ProfileSection.WISHLIST) }
                )
                ThinDivider(outline)
                ProfileRow(
                    icon = Icons.Default.Place,
                    iconTint = TraviraSuccess,
                    iconBg = if (isDark) Color(0xFF1B3A2A) else TraviraSuccessBg,
                    title = "Visited Destinations",
                    subtitle = if (visitedCount > 0) "$visitedCount places explored" else "Mark places as visited",
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    onClick = { requireLoginThen(ProfileSection.VISITED) }
                )
                ThinDivider(outline)
                ProfileRow(
                    icon = Icons.Default.RateReview,
                    iconTint = TraviraStar,
                    iconBg = if (isDark) Color(0xFF3A3010) else TraviraStarBg,
                    title = "My Reviews",
                    subtitle = if (reviewsCount > 0) "$reviewsCount review${if (reviewsCount == 1) "" else "s"}" else "Rate places to add reviews",
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    onClick = { requireLoginThen(ProfileSection.REVIEWS) }
                )
            }

            Spacer(Modifier.height(12.dp))

            // Preferences — Appearance only
            SectionHeader(
                title = "Preferences",
                caption = "Customization",
                textPrimary = textPrimary,
                textTertiary = textTertiary
            )
            ProfileSectionCard(cardBg = cardBg, outline = outline) {
                val themeLabel = when (themeMode) {
                    "light" -> "Light"
                    "dark" -> "Dark"
                    else -> "System"
                }
                val themeIcon = when (themeMode) {
                    "dark" -> Icons.Default.DarkMode
                    else -> Icons.Default.LightMode
                }
                ProfileRow(
                    icon = themeIcon,
                    iconTint = TraviraBlue,
                    iconBg = surfaceLow,
                    title = "Appearance",
                    subtitle = "$themeLabel · tap to change",
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    onClick = { cycleTheme() }
                )
            }

            Spacer(Modifier.height(12.dp))

            // Account
            SectionHeader(
                title = "Account",
                caption = "System",
                textPrimary = textPrimary,
                textTertiary = textTertiary
            )
            ProfileSectionCard(cardBg = cardBg, outline = outline) {
                ProfileRow(
                    icon = Icons.Default.HelpOutline,
                    iconTint = TraviraMutedIcon,
                    iconBg = surfaceLow,
                    title = "Help & Support",
                    subtitle = "FAQs and contact",
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    onClick = {
                        openUrl(context, "mailto:support@travira.app")
                    }
                )
                if (isLoggedIn) {
                    ThinDivider(outline)
                    ProfileRow(
                        icon = Icons.AutoMirrored.Filled.Logout,
                        iconTint = TraviraError,
                        iconBg = if (isDark) Color(0xFF3A1A1A) else Color(0xFFFFEBEE),
                        title = "Log Out",
                        subtitle = "$displayName · Travira Account",
                        textPrimary = TraviraError,
                        textSecondary = textSecondary,
                        showChevron = true,
                        onClick = onLogoutClick
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Contact details
            SectionHeader(
                title = "Contact",
                caption = "Reach us",
                textPrimary = textPrimary,
                textTertiary = textTertiary
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(cardBg)
                    .padding(horizontal = 12.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ContactIconButton(
                    label = "Email",
                    icon = Icons.Default.Email,
                    tint = TraviraBlue,
                    bg = surfaceLow
                ) { openUrl(context, "mailto:support@travira.app") }
                ContactIconButton(
                    label = "Call",
                    icon = Icons.Default.Call,
                    tint = TraviraSuccess,
                    bg = if (isDark) Color(0xFF1B3A2A) else TraviraSuccessBg
                ) { openUrl(context, "tel:+911234567890") }
                ContactIconButton(
                    label = "WhatsApp",
                    icon = Icons.Default.Favorite,
                    tint = Color(0xFF25D366),
                    bg = if (isDark) Color(0xFF1B3A2A) else Color(0xFFE8F5E9)
                ) { openUrl(context, "https://wa.me/911234567890") }
                ContactIconButton(
                    label = "Insta",
                    icon = Icons.Default.TravelExplore,
                    tint = Color(0xFFE1306C),
                    bg = if (isDark) Color(0xFF3A1A28) else TraviraHeartBg
                ) { openUrl(context, "https://instagram.com/travira") }
                ContactIconButton(
                    label = "GitHub",
                    icon = Icons.Default.Code,
                    tint = textPrimary,
                    bg = surfaceLow
                ) { openUrl(context, "https://github.com/travira") }
                ContactIconButton(
                    label = "Pinterest",
                    icon = Icons.Default.Star,
                    tint = Color(0xFFE60023),
                    bg = if (isDark) Color(0xFF3A1A1A) else Color(0xFFFFEBEE)
                ) { openUrl(context, "https://pinterest.com/travira") }
            }

            Spacer(Modifier.height(16.dp))

            // Footer
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.TravelExplore,
                    contentDescription = null,
                    tint = TraviraBlue.copy(alpha = 0.5f),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Travira v3.4.2 • Crafted for Wanderlust",
                    fontSize = 12.sp,
                    color = textTertiary,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(20.dp))
        }

        // Toast
        toastMsg?.let { msg ->
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 100.dp)
                    .padding(horizontal = 24.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(TraviraBlue)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .clickable { toastMsg = null }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(msg, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
            }
            // auto-dismiss after short delay is better with LaunchedEffect — keep simple tap-to-dismiss
        }
    }
}

@Composable
private fun ContactIconButton(
    label: String,
    icon: ImageVector,
    tint: Color,
    bg: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(bg),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 9.sp,
            color = TraviraTextTertiary,
            maxLines = 1
        )
    }
}

@Composable
private fun StatPill(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    label: String,
    value: String,
    cardBg: Color,
    textPrimary: Color,
    textTertiary: Color,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(cardBg)
            .clickable(onClick = onClick)
            .padding(14.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = value,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = textPrimary
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = textTertiary,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun SectionHeader(
    title: String,
    caption: String,
    textPrimary: Color,
    textTertiary: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Text(
            text = title,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            color = textPrimary
        )
        Text(
            text = caption.uppercase(),
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.8.sp,
            color = textTertiary
        )
    }
}

@Composable
private fun ProfileSectionCard(
    cardBg: Color,
    outline: Color,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(cardBg)
            .border(1.dp, outline.copy(alpha = 0.45f), RoundedCornerShape(20.dp))
    ) {
        content()
    }
}

@Composable
private fun ProfileRow(
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    title: String,
    subtitle: String,
    textPrimary: Color,
    textSecondary: Color,
    trailingBadge: String? = null,
    showChevron: Boolean = true,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconBubble(icon = icon, tint = iconTint, bg = iconBg)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = textPrimary
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (trailingBadge != null) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(TraviraBlue.copy(alpha = 0.12f))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = trailingBadge,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = TraviraBlue,
                    letterSpacing = 0.5.sp
                )
            }
            Spacer(Modifier.width(6.dp))
        }
        if (showChevron) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = TraviraTextTertiary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun IconBubble(icon: ImageVector, tint: Color, bg: Color) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bg),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun ThinDivider(color: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .height(1.dp)
            .background(color.copy(alpha = 0.5f))
    )
}

private fun openUrl(context: android.content.Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (_: Exception) {
    }
}

@Preview(showBackground = true, name = "Profile – Guest", showSystemUi = true)
@Composable
private fun ProfileScreenGuestPreview() {
    TraviraTheme {
        ProfileScreen(
            isLoggedIn = false,
            user = null,
            onLoginClick = {},
            onLogoutClick = {}
        )
    }
}

@Preview(showBackground = true, name = "Profile – User", showSystemUi = true)
@Composable
private fun ProfileScreenUserPreview() {
    TraviraTheme {
        ProfileScreen(
            isLoggedIn = true,
            user = User(
                id = "1",
                name = "Preet Patel",
                email = "preetp0270@gmail.com",
                role = "user",
                location = "Surat",
                bio = "Explore smarter. Discover deeper. Travel with confidence."
            ),
            onLoginClick = {},
            onLogoutClick = {}
        )
    }
}

@Preview(showBackground = true, name = "Profile – Admin", showSystemUi = true)
@Composable
private fun ProfileScreenAdminPreview() {
    TraviraTheme {
        ProfileScreen(
            isLoggedIn = true,
            user = User(
                id = "1",
                name = "Preet Patel",
                email = "preetp0270@gmail.com",
                role = "admin",
                location = "Surat"
            ),
            onLoginClick = {},
            onLogoutClick = {}
        )
    }
}
