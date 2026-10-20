package io.github.d4rthr4tz3.apviewer.ui.theme

import androidx.compose.ui.graphics.Color

/** Color tokens of the design, see docs/product/design.md. */
object ApColors {
    val Background = Color(0xFF141414)
    val Surface = Color(0xFF1E1E1E)
    val Divider = Color(0xFF262626)
    val InputBorder = Color(0xFF2E2E2E)
    val SegmentActive = Color(0xFF3A3A3A)
    val TextPrimary = Color(0xFFEDEDED)
    val TextSecondary = Color(0xFFA8A8A8)
    val TextTertiary = Color(0xFF8C8C8C)
    val Warning = Color(0xFFE3B341)

    /** Only for problems, such as a refused connection. */
    val Error = Color(0xFFF07C7C)

    /** Border of a card that reports a problem. */
    val ErrorBorder = Color(0xFF3A2626)
}

/** Colors that make feed, hint and notification content readable. */
object ContentColors {
    val Progression = Color(0xFFB58CFF)
    val Useful = Color(0xFF7AA2F7)
    val Filler = Color(0xFF4FD6D6)
    val Trap = Color(0xFFFF7A50)

    val OwnSlot = Color(0xFFF06AE8)
    val OtherPlayer = Color(0xFFD6D6D6)
    val Location = Color(0xFF6CCB8E)
    val Entrance = Color(0xFFA4B4C4)

    val HintPriority = Color(0xFFE8C35A)
    val HintOpen = Color(0xFFB0B0B0)
    val HintAvoid = Color(0xFFF07C7C)
    val HintNoNeed = Color(0xFF8C8C8C)
    val HintFound = Color(0xFF6CCB8E)

    /** Feed row background for items sent to one of the user's slots. */
    val HighlightForMe = Color(0x1FF06AE8)

    /** Feed row background for items one of the user's slots sent. */
    val HighlightFromMe = Color(0x0DF06AE8)
}
