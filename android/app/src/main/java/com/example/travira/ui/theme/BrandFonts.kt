package com.example.travira.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.example.travira.R

/**
 * Google Font **Poppins** (bundled under res/font — no API key / env needed).
 *
 * To switch brand font later:
 * 1. Drop new .ttf files into `app/src/main/res/font/` (lowercase_with_underscores)
 * 2. Update the Font(...) resources below
 *
 * Used for the "Travira" title in the home top pill.
 */
val TraviraBrandFont = FontFamily(
    Font(R.font.borel_regular, weight = FontWeight.Normal)
)
