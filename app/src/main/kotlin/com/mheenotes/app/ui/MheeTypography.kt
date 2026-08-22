package com.mheenotes.app.ui

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val MheeTypography = Typography(
    headlineLarge = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.W600, letterSpacing = (-0.5).sp),
    headlineMedium = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.W600, letterSpacing = (-0.3).sp),
    headlineSmall = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.W600),
    titleLarge = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.W500),
    titleMedium = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.W500),
    titleSmall = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.W500),
    bodyLarge = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.W400, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.W400, lineHeight = 20.sp),
    bodySmall = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.W400, lineHeight = 16.sp),
    labelLarge = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.W500, letterSpacing = 0.3.sp),
    labelMedium = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.W500, letterSpacing = 0.5.sp),
    labelSmall = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.W500, letterSpacing = 0.5.sp),
)
