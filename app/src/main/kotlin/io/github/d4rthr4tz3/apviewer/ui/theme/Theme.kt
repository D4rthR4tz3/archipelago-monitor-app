package io.github.d4rthr4tz3.apviewer.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val ColorScheme = darkColorScheme(
    primary = ApColors.TextPrimary,
    onPrimary = ApColors.Background,
    secondary = ApColors.TextSecondary,
    onSecondary = ApColors.Background,
    background = ApColors.Background,
    onBackground = ApColors.TextPrimary,
    surface = ApColors.Background,
    onSurface = ApColors.TextPrimary,
    surfaceVariant = ApColors.Surface,
    onSurfaceVariant = ApColors.TextSecondary,
    surfaceContainer = ApColors.Surface,
    surfaceContainerHigh = ApColors.Surface,
    surfaceContainerHighest = ApColors.SegmentActive,
    outline = ApColors.InputBorder,
    outlineVariant = ApColors.Divider,
    error = ApColors.Error,
    onError = ApColors.Background,
)

/** Controls use 8 dp corners, surfaces 12 dp. */
private val ApShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(12.dp),
    extraLarge = RoundedCornerShape(12.dp),
)

/** The app's only theme: dark neutral gray, Geist, no dynamic colors. */
@Composable
fun ApViewerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ColorScheme,
        typography = ApTypography,
        shapes = ApShapes,
        content = content,
    )
}
