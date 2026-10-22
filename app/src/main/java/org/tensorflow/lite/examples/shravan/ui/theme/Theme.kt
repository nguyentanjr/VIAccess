package org.tensorflow.lite.examples.shravan.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.text.googlefonts.Font
import org.tensorflow.lite.examples.shravan.R

val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

val fontName = GoogleFont("Inter")

val InterFontFamily = FontFamily(
    Font(googleFont = fontName, fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = fontName, fontProvider = provider, weight = FontWeight.SemiBold),
    Font(googleFont = fontName, fontProvider = provider, weight = FontWeight.Bold)
)

// Legible Typography for visually impaired (bold, clear sans-serif)
private val AppTypography = Typography(
    displayLarge = TextStyle(fontFamily = InterFontFamily),
    displayMedium = TextStyle(fontFamily = InterFontFamily),
    displaySmall = TextStyle(fontFamily = InterFontFamily),
    headlineLarge = TextStyle(fontFamily = InterFontFamily),
    headlineMedium = TextStyle(fontFamily = InterFontFamily),
    headlineSmall = TextStyle(fontFamily = InterFontFamily),
    titleLarge = TextStyle(fontFamily = InterFontFamily),
    titleMedium = TextStyle(fontFamily = InterFontFamily),
    titleSmall = TextStyle(fontFamily = InterFontFamily),
    bodyLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(fontFamily = InterFontFamily),
    bodySmall = TextStyle(fontFamily = InterFontFamily),
    labelLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(fontFamily = InterFontFamily),
    labelSmall = TextStyle(fontFamily = InterFontFamily)
)

@Composable
fun ShravanTheme(
    themeIndex: Int,
    content: @Composable () -> Unit
) {
    val theme = ThemeCatalog.themes.getOrElse(themeIndex) { ThemeCatalog.themes[3] }
    val colors = theme.colors

    val colorScheme = lightColorScheme(
        primary = colors[2],
        secondary = colors[3],
        tertiary = colors[1],
        background = colors[4],
        surface = colors[4],
        onPrimary = colors[0],
        onSecondary = colors[0],
        onTertiary = colors[0],
        onBackground = colors[0],
        onSurface = colors[0],
    )
    
    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
