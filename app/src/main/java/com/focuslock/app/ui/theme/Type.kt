package com.focuslock.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.focuslock.app.R

/** Headlines: Anton (one weight, mapped to every weight so Compose never fakes bold). */
val AntonFamily = FontFamily(
    Font(R.font.anton, FontWeight.Normal),
    Font(R.font.anton, FontWeight.Medium),
    Font(R.font.anton, FontWeight.SemiBold),
    Font(R.font.anton, FontWeight.Bold)
)

/** Body: Inter. */
val InterFamily = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold)
)

/** Labels, numbers, small data: Space Mono. */
val MonoFamily = FontFamily(
    Font(R.font.space_mono_regular, FontWeight.Normal),
    Font(R.font.space_mono_regular, FontWeight.Medium),
    Font(R.font.space_mono_bold, FontWeight.SemiBold),
    Font(R.font.space_mono_bold, FontWeight.Bold)
)

val FocusLockTypography = Typography(
    headlineLarge = TextStyle(fontFamily = AntonFamily, fontWeight = FontWeight.Normal, fontSize = 40.sp, letterSpacing = 0.4.sp),
    headlineMedium = TextStyle(fontFamily = AntonFamily, fontWeight = FontWeight.Normal, fontSize = 30.sp, letterSpacing = 0.3.sp),
    headlineSmall = TextStyle(fontFamily = AntonFamily, fontWeight = FontWeight.Normal, fontSize = 24.sp, letterSpacing = 0.3.sp),
    titleLarge = TextStyle(fontFamily = AntonFamily, fontWeight = FontWeight.Normal, fontSize = 22.sp, letterSpacing = 0.3.sp),
    titleMedium = TextStyle(fontFamily = AntonFamily, fontWeight = FontWeight.Normal, fontSize = 17.sp, letterSpacing = 0.3.sp),
    titleSmall = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
    bodyLarge = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 21.sp),
    bodySmall = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.Normal, fontSize = 12.sp),
    labelLarge = TextStyle(fontFamily = MonoFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp, letterSpacing = 0.8.sp),
    labelMedium = TextStyle(fontFamily = MonoFamily, fontWeight = FontWeight.Normal, fontSize = 12.sp, letterSpacing = 0.8.sp),
    labelSmall = TextStyle(fontFamily = MonoFamily, fontWeight = FontWeight.Normal, fontSize = 11.sp, letterSpacing = 0.8.sp)
)
