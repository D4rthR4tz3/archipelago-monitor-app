package io.github.d4rthr4tz3.apviewer.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/** Colors of message segments, modeled on the Archipelago text client. */
@Immutable
data class FeedColors(
    val ownPlayer: Color,
    val player: Color,
    val item: Color,
    val progressionItem: Color,
    val usefulItem: Color,
    val trapItem: Color,
    val location: Color,
    val entrance: Color,
) {
    companion object {
        val Dark = FeedColors(
            ownPlayer = Color(0xFFEE00EE),
            player = Color(0xFFFAFAD2),
            item = Color(0xFF00EEEE),
            progressionItem = Color(0xFFAF99EF),
            usefulItem = Color(0xFF6D8BE8),
            trapItem = Color(0xFFFA8072),
            location = Color(0xFF00FF7F),
            entrance = Color(0xFF6495ED),
        )

        val Light = FeedColors(
            ownPlayer = Color(0xFF9C009C),
            player = Color(0xFF8A6D00),
            item = Color(0xFF007C7C),
            progressionItem = Color(0xFF6A4FC2),
            usefulItem = Color(0xFF2F55C4),
            trapItem = Color(0xFFC0392B),
            location = Color(0xFF00804A),
            entrance = Color(0xFF2D63C8),
        )
    }
}

/** Returns the [FeedColors] that match the system theme. */
@Composable
fun feedColors(): FeedColors = if (isSystemInDarkTheme()) FeedColors.Dark else FeedColors.Light
