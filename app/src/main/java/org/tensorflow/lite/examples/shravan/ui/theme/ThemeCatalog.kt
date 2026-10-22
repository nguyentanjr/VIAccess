package org.tensorflow.lite.examples.shravan.ui.theme

import androidx.compose.ui.graphics.Color

data class AppTheme(
    val name: String,
    val colors: List<Color>
)

object ThemeCatalog {
    val themes = listOf(
        AppTheme("Midnight Mustard", listOf(Color(0xFF0B0B0D), Color(0xFF1E1F22), Color(0xFFCAA400), Color(0xFFFFD34D), Color(0xFFF2EFE8))),
        AppTheme("Taxi Stripe", listOf(Color(0xFF0A0A0A), Color(0xFF2B2B2B), Color(0xFFFFCC00), Color(0xFFFFF1A8), Color(0xFFFFFFFF))),
        AppTheme("Vintage Newspaper", listOf(Color(0xFF121212), Color(0xFF3A3A3A), Color(0xFFD6B300), Color(0xFFF5E7A1), Color(0xFFF3F0E6))),
        AppTheme("Neon Hazard", listOf(Color(0xFF050506), Color(0xFF22222A), Color(0xFFFFE600), Color(0xFFA8FF3E), Color(0xFFE9E9EF))),
        AppTheme("Desert Bee", listOf(Color(0xFF0F0F10), Color(0xFF4A3B2B), Color(0xFFD4A300), Color(0xFFF2C14E), Color(0xFFFFF3D6))),
        AppTheme("Art Deco Gild", listOf(Color(0xFF0C0C0F), Color(0xFF2A2A33), Color(0xFFB58B00), Color(0xFFFFDA66), Color(0xFFF7F3EA))),
        AppTheme("Solar Eclipse", listOf(Color(0xFF070708), Color(0xFF1B1C20), Color(0xFFFFBF00), Color(0xFFFFD86B), Color(0xFFD7DDE6))),
        AppTheme("Industrial Caution", listOf(Color(0xFF0F1012), Color(0xFF2F3136), Color(0xFFF6C500), Color(0xFFFFE07A), Color(0xFFC9CED6))),
        AppTheme("Honeyed Charcoal", listOf(Color(0xFF111113), Color(0xFF34363C), Color(0xFFCFA300), Color(0xFFF7D06B), Color(0xFFFFF6E8))),
        AppTheme("Minimal Contrast", listOf(Color(0xFF0D0D0E), Color(0xFF2A2A2C), Color(0xFFE3B400), Color(0xFFF6E6A6), Color(0xFFF8F8F8))),
        AppTheme("Retro Arcade", listOf(Color(0xFF050508), Color(0xFF1C1C2A), Color(0xFFFFD400), Color(0xFFFF4FD8), Color(0xFFE6E6FF))),
        AppTheme("Sporty Chevron", listOf(Color(0xFF0A0A0B), Color(0xFF2D2E32), Color(0xFFFFCC33), Color(0xFFFFF2C2), Color(0xFFE7EEF6))),
        AppTheme("Warm Granite", listOf(Color(0xFF101114), Color(0xFF3B3F46), Color(0xFFD9B100), Color(0xFFF3DA87), Color(0xFFF2F4F6))),
        AppTheme("Golden Ink", listOf(Color(0xFF0B0B0C), Color(0xFF24262B), Color(0xFFF1C40F), Color(0xFFFFE8A3), Color(0xFFFAF7F0))),
        AppTheme("Cosmic Gold", listOf(Color(0xFF06060A), Color(0xFF1A1B22), Color(0xFFF9C900), Color(0xFF9AA3FF), Color(0xFFE9ECFF))),
        AppTheme("Sunlit Asphalt", listOf(Color(0xFF0D0E10), Color(0xFF30323A), Color(0xFFFFBE0B), Color(0xFFFFE29A), Color(0xFFF0F2F5))),
        AppTheme("Dandelion Noir", listOf(Color(0xFF0A0A0A), Color(0xFF2B2B2B), Color(0xFFF4D000), Color(0xFFFFF2B0), Color(0xFFF7F7F7))),
        AppTheme("Muted Saffron", listOf(Color(0xFF101010), Color(0xFF3A3A3D), Color(0xFFC9A227), Color(0xFFF3E2B5), Color(0xFFECE9E1))),
        AppTheme("Lemon Graphite", listOf(Color(0xFF0C0D0F), Color(0xFF3C4048), Color(0xFFFFE066), Color(0xFFFFF3B3), Color(0xFFE5E9F0))),
        AppTheme("Golden Hour Studio", listOf(Color(0xFF0B0B0B), Color(0xFF2F2A24), Color(0xFFF2B705), Color(0xFFFFD08A), Color(0xFFFFF2E4)))
    )
}
