package io.github.d4rthr4tz3.apviewer.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import io.github.d4rthr4tz3.apviewer.R

@OptIn(ExperimentalTextApi::class)
private fun variableFonts(resId: Int, vararg weights: FontWeight): FontFamily = FontFamily(
    weights.map { weight ->
        Font(
            resId = resId,
            weight = weight,
            variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
        )
    },
)

/** Geist, the text face of the app. */
val Geist: FontFamily = variableFonts(
    R.font.geist,
    FontWeight.Normal,
    FontWeight.Medium,
    FontWeight.SemiBold,
)

/** Geist Mono, used for the classic feed and timestamps. */
val GeistMono: FontFamily = variableFonts(
    R.font.geist_mono,
    FontWeight.Normal,
    FontWeight.Medium,
    FontWeight.SemiBold,
)

private val Default = Typography()

/** Material type scale set in Geist. */
val ApTypography: Typography = Typography(
    displayLarge = Default.displayLarge.copy(fontFamily = Geist),
    displayMedium = Default.displayMedium.copy(fontFamily = Geist),
    displaySmall = Default.displaySmall.copy(fontFamily = Geist),
    headlineLarge = Default.headlineLarge.copy(fontFamily = Geist),
    headlineMedium = Default.headlineMedium.copy(fontFamily = Geist),
    headlineSmall = Default.headlineSmall.copy(fontFamily = Geist),
    titleLarge = TextStyle(
        fontFamily = Geist,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        letterSpacing = (-0.2).sp,
    ),
    titleMedium = TextStyle(fontFamily = Geist, fontWeight = FontWeight.SemiBold, fontSize = 17.sp),
    titleSmall = TextStyle(fontFamily = Geist, fontWeight = FontWeight.Medium, fontSize = 14.sp),
    bodyLarge = TextStyle(fontFamily = Geist, fontSize = 16.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontFamily = Geist, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = Geist, fontSize = 13.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontFamily = Geist, fontWeight = FontWeight.Medium, fontSize = 15.sp),
    labelMedium = TextStyle(fontFamily = Geist, fontWeight = FontWeight.Medium, fontSize = 13.sp),
    labelSmall = TextStyle(fontFamily = Geist, fontSize = 12.sp),
)

/** Style of rows in the classic feed. */
val FeedMonoStyle = TextStyle(fontFamily = GeistMono, fontSize = 12.sp, lineHeight = 17.sp)
