package com.project.lol.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.project.lol.R

val Saira = FontFamily(
    Font(R.font.saira_thin, FontWeight.Thin),
    Font(R.font.saira_extralight, FontWeight.ExtraLight),
    Font(R.font.saira_light, FontWeight.Light),
    Font(R.font.saira_regular, FontWeight.Normal),
    Font(R.font.saira_medium, FontWeight.Medium),
    Font(R.font.saira_semibold, FontWeight.SemiBold),
    Font(R.font.saira_bold, FontWeight.Bold),
    Font(R.font.saira_extrabold, FontWeight.ExtraBold),
    Font(R.font.saira_black, FontWeight.Black),
)

val SairaTypography = Typography(
    displayLarge = TextStyle(fontFamily = Saira, fontWeight = FontWeight.Normal),
    displayMedium = TextStyle(fontFamily = Saira, fontWeight = FontWeight.Normal),
    displaySmall = TextStyle(fontFamily = Saira, fontWeight = FontWeight.Normal),
    headlineLarge = TextStyle(fontFamily = Saira, fontWeight = FontWeight.Normal),
    headlineMedium = TextStyle(fontFamily = Saira, fontWeight = FontWeight.Normal),
    headlineSmall = TextStyle(fontFamily = Saira, fontWeight = FontWeight.Normal),
    titleLarge = TextStyle(fontFamily = Saira, fontWeight = FontWeight.Bold),
    titleMedium = TextStyle(fontFamily = Saira, fontWeight = FontWeight.SemiBold),
    titleSmall = TextStyle(fontFamily = Saira, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontFamily = Saira, fontWeight = FontWeight.Normal),
    bodyMedium = TextStyle(fontFamily = Saira, fontWeight = FontWeight.Normal),
    bodySmall = TextStyle(fontFamily = Saira, fontWeight = FontWeight.Normal),
    labelLarge = TextStyle(fontFamily = Saira, fontWeight = FontWeight.Medium),
    labelMedium = TextStyle(fontFamily = Saira, fontWeight = FontWeight.Medium),
    labelSmall = TextStyle(fontFamily = Saira, fontWeight = FontWeight.Medium),
)
