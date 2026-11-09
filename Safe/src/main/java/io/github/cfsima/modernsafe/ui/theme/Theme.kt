package io.github.cfsima.modernsafe.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

const val THEME_DYNAMIC = "dynamic"
const val THEME_EMERALD = "emerald"
const val THEME_SAPPHIRE = "sapphire"
const val THEME_COPPER = "copper"

const val DARK_MODE_SYSTEM = "system"
const val DARK_MODE_LIGHT = "light"
const val DARK_MODE_DARK = "dark"

// 1. Modern Emerald Vault Schemes
val EmeraldDarkColorScheme = darkColorScheme(
    primary = EmeraldDarkPrimary,
    onPrimary = EmeraldDarkOnPrimary,
    primaryContainer = EmeraldDarkPrimaryContainer,
    onPrimaryContainer = EmeraldDarkOnPrimaryContainer,
    secondary = EmeraldDarkSecondary,
    onSecondary = EmeraldDarkOnSecondary,
    secondaryContainer = EmeraldDarkSecondaryContainer,
    onSecondaryContainer = EmeraldDarkOnSecondaryContainer,
    tertiary = EmeraldDarkTertiary,
    onTertiary = EmeraldDarkOnTertiary,
    tertiaryContainer = EmeraldDarkTertiaryContainer,
    onTertiaryContainer = EmeraldDarkOnTertiaryContainer,
    background = EmeraldDarkBackground,
    onBackground = EmeraldDarkOnBackground,
    surface = EmeraldDarkSurface,
    onSurface = EmeraldDarkOnSurface,
    surfaceVariant = EmeraldDarkSurfaceVariant,
    onSurfaceVariant = EmeraldDarkOnSurfaceVariant,
    outline = EmeraldDarkOutline,
    outlineVariant = EmeraldDarkOutlineVariant
)

val EmeraldLightColorScheme = lightColorScheme(
    primary = EmeraldLightPrimary,
    onPrimary = EmeraldLightOnPrimary,
    primaryContainer = EmeraldLightPrimaryContainer,
    onPrimaryContainer = EmeraldLightOnPrimaryContainer,
    secondary = EmeraldLightSecondary,
    onSecondary = EmeraldLightOnSecondary,
    secondaryContainer = EmeraldLightSecondaryContainer,
    onSecondaryContainer = EmeraldLightOnSecondaryContainer,
    tertiary = EmeraldLightTertiary,
    onTertiary = EmeraldLightOnTertiary,
    tertiaryContainer = EmeraldLightTertiaryContainer,
    onTertiaryContainer = EmeraldLightOnTertiaryContainer,
    background = EmeraldLightBackground,
    onBackground = EmeraldLightOnBackground,
    surface = EmeraldLightSurface,
    onSurface = EmeraldLightOnSurface,
    surfaceVariant = EmeraldLightSurfaceVariant,
    onSurfaceVariant = EmeraldLightOnSurfaceVariant,
    outline = EmeraldLightOutline,
    outlineVariant = EmeraldLightOutlineVariant
)

// 2. Titanium Sapphire Schemes
val SapphireDarkColorScheme = darkColorScheme(
    primary = SapphireDarkPrimary,
    onPrimary = SapphireDarkOnPrimary,
    primaryContainer = SapphireDarkPrimaryContainer,
    onPrimaryContainer = SapphireDarkOnPrimaryContainer,
    secondary = SapphireDarkSecondary,
    onSecondary = SapphireDarkOnSecondary,
    secondaryContainer = SapphireDarkSecondaryContainer,
    onSecondaryContainer = SapphireDarkOnSecondaryContainer,
    tertiary = SapphireDarkTertiary,
    onTertiary = SapphireDarkOnTertiary,
    tertiaryContainer = SapphireDarkTertiaryContainer,
    onTertiaryContainer = SapphireDarkOnTertiaryContainer,
    background = SapphireDarkBackground,
    onBackground = SapphireDarkOnBackground,
    surface = SapphireDarkSurface,
    onSurface = SapphireDarkOnSurface,
    surfaceVariant = SapphireDarkSurfaceVariant,
    onSurfaceVariant = SapphireDarkOnSurfaceVariant,
    outline = SapphireDarkOutline,
    outlineVariant = SapphireDarkOutlineVariant
)

val SapphireLightColorScheme = lightColorScheme(
    primary = SapphireLightPrimary,
    onPrimary = SapphireLightOnPrimary,
    primaryContainer = SapphireLightPrimaryContainer,
    onPrimaryContainer = SapphireLightOnPrimaryContainer,
    secondary = SapphireLightSecondary,
    onSecondary = SapphireLightOnSecondary,
    secondaryContainer = SapphireLightSecondaryContainer,
    onSecondaryContainer = SapphireLightOnSecondaryContainer,
    tertiary = SapphireLightTertiary,
    onTertiary = SapphireLightOnTertiary,
    tertiaryContainer = SapphireLightTertiaryContainer,
    onTertiaryContainer = SapphireLightOnTertiaryContainer,
    background = SapphireLightBackground,
    onBackground = SapphireLightOnBackground,
    surface = SapphireLightSurface,
    onSurface = SapphireLightOnSurface,
    surfaceVariant = SapphireLightSurfaceVariant,
    onSurfaceVariant = SapphireLightOnSurfaceVariant,
    outline = SapphireLightOutline,
    outlineVariant = SapphireLightOutlineVariant
)

// 3. Nordic Copper Schemes
val CopperDarkColorScheme = darkColorScheme(
    primary = CopperDarkPrimary,
    onPrimary = CopperDarkOnPrimary,
    primaryContainer = CopperDarkPrimaryContainer,
    onPrimaryContainer = CopperDarkOnPrimaryContainer,
    secondary = CopperDarkSecondary,
    onSecondary = CopperDarkOnSecondary,
    secondaryContainer = CopperDarkSecondaryContainer,
    onSecondaryContainer = CopperDarkOnSecondaryContainer,
    tertiary = CopperDarkTertiary,
    onTertiary = CopperDarkOnTertiary,
    tertiaryContainer = CopperDarkTertiaryContainer,
    onTertiaryContainer = CopperDarkOnTertiaryContainer,
    background = CopperDarkBackground,
    onBackground = CopperDarkOnBackground,
    surface = CopperDarkSurface,
    onSurface = CopperDarkOnSurface,
    surfaceVariant = CopperDarkSurfaceVariant,
    onSurfaceVariant = CopperDarkOnSurfaceVariant,
    outline = CopperDarkOutline,
    outlineVariant = CopperDarkOutlineVariant
)

val CopperLightColorScheme = lightColorScheme(
    primary = CopperLightPrimary,
    onPrimary = CopperLightOnPrimary,
    primaryContainer = CopperLightPrimaryContainer,
    onPrimaryContainer = CopperLightOnPrimaryContainer,
    secondary = CopperLightSecondary,
    onSecondary = CopperLightOnSecondary,
    secondaryContainer = CopperLightSecondaryContainer,
    onSecondaryContainer = CopperLightOnSecondaryContainer,
    tertiary = CopperLightTertiary,
    onTertiary = CopperLightOnTertiary,
    tertiaryContainer = CopperLightTertiaryContainer,
    onTertiaryContainer = CopperLightOnTertiaryContainer,
    background = CopperLightBackground,
    onBackground = CopperLightOnBackground,
    surface = CopperLightSurface,
    onSurface = CopperLightOnSurface,
    surfaceVariant = CopperLightSurfaceVariant,
    onSurfaceVariant = CopperLightOnSurfaceVariant,
    outline = CopperLightOutline,
    outlineVariant = CopperLightOutlineVariant
)

// 4. Material You Fallback Schemes
val DynamicDarkColorScheme = darkColorScheme(
    primary = DynamicDarkPrimary,
    onPrimary = DynamicDarkOnPrimary,
    primaryContainer = DynamicDarkPrimaryContainer,
    onPrimaryContainer = DynamicDarkOnPrimaryContainer,
    secondary = DynamicDarkSecondary,
    onSecondary = DynamicDarkOnSecondary,
    secondaryContainer = DynamicDarkSecondaryContainer,
    onSecondaryContainer = DynamicDarkOnSecondaryContainer,
    tertiary = DynamicDarkTertiary,
    onTertiary = DynamicDarkOnTertiary,
    tertiaryContainer = DynamicDarkTertiaryContainer,
    onTertiaryContainer = DynamicDarkOnTertiaryContainer,
    background = DynamicDarkBackground,
    onBackground = DynamicDarkOnBackground,
    surface = DynamicDarkSurface,
    onSurface = DynamicDarkOnSurface,
    surfaceVariant = DynamicDarkSurfaceVariant,
    onSurfaceVariant = DynamicDarkOnSurfaceVariant,
    outline = DynamicDarkOutline,
    outlineVariant = DynamicDarkOutlineVariant
)

val DynamicLightColorScheme = lightColorScheme(
    primary = DynamicLightPrimary,
    onPrimary = DynamicLightOnPrimary,
    primaryContainer = DynamicLightPrimaryContainer,
    onPrimaryContainer = DynamicLightOnPrimaryContainer,
    secondary = DynamicLightSecondary,
    onSecondary = DynamicLightOnSecondary,
    secondaryContainer = DynamicLightSecondaryContainer,
    onSecondaryContainer = DynamicLightOnSecondaryContainer,
    tertiary = DynamicLightTertiary,
    onTertiary = DynamicLightOnTertiary,
    tertiaryContainer = DynamicLightTertiaryContainer,
    onTertiaryContainer = DynamicLightOnTertiaryContainer,
    background = DynamicLightBackground,
    onBackground = DynamicLightOnBackground,
    surface = DynamicLightSurface,
    onSurface = DynamicLightOnSurface,
    surfaceVariant = DynamicLightSurfaceVariant,
    onSurfaceVariant = DynamicLightOnSurfaceVariant,
    outline = DynamicLightOutline,
    outlineVariant = DynamicLightOutlineVariant
)

val ModernSafeShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

val ModernSafeTypography = Typography(
    headlineLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 26.sp,
        lineHeight = 34.sp,
        letterSpacing = 0.sp
    ),
    headlineSmall = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    titleSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)

@Composable
fun ModernSafeTheme(
    themePalette: String = THEME_DYNAMIC,
    darkMode: String = DARK_MODE_SYSTEM,
    darkTheme: Boolean = when (darkMode) {
        DARK_MODE_DARK -> true
        DARK_MODE_LIGHT -> false
        else -> isSystemInDarkTheme()
    },
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when (themePalette) {
        THEME_EMERALD -> if (darkTheme) EmeraldDarkColorScheme else EmeraldLightColorScheme
        THEME_SAPPHIRE -> if (darkTheme) SapphireDarkColorScheme else SapphireLightColorScheme
        THEME_COPPER -> if (darkTheme) CopperDarkColorScheme else CopperLightColorScheme
        else -> { // THEME_DYNAMIC
            if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            } else {
                if (darkTheme) DynamicDarkColorScheme else DynamicLightColorScheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        shapes = ModernSafeShapes,
        typography = ModernSafeTypography,
        content = content
    )
}
