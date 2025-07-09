package com.golfswing.vro.pixel.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Golf-themed semantic colors
object GolfColors {
    // Primary colors - Golf green theme
    val Primary = Color(0xFF2E7D32)  // Golf green
    val OnPrimary = Color(0xFFFFFFFF)
    val PrimaryContainer = Color(0xFF66BB6A)
    val OnPrimaryContainer = Color(0xFF000000)
    
    // Secondary colors - Complementary earth tones
    val Secondary = Color(0xFF8D6E63)  // Earth brown
    val OnSecondary = Color(0xFFFFFFFF)
    val SecondaryContainer = Color(0xFFBCAAA4)
    val OnSecondaryContainer = Color(0xFF000000)
    
    // Tertiary colors - Sky blue for contrast
    val Tertiary = Color(0xFF0277BD)  // Sky blue
    val OnTertiary = Color(0xFFFFFFFF)
    val TertiaryContainer = Color(0xFF81D4FA)
    val OnTertiaryContainer = Color(0xFF000000)
    
    // Surface colors
    val Surface = Color(0xFFF5F5F5)
    val OnSurface = Color(0xFF1C1C1C)
    val SurfaceVariant = Color(0xFFE8E8E8)
    val OnSurfaceVariant = Color(0xFF444444)
    
    // Background colors
    val Background = Color(0xFFFAFAFA)
    val OnBackground = Color(0xFF1C1C1C)
    
    // Error colors
    val Error = Color(0xFFD32F2F)
    val OnError = Color(0xFFFFFFFF)
    val ErrorContainer = Color(0xFFFFCDD2)
    val OnErrorContainer = Color(0xFF5F2120)
    
    // Outline colors
    val Outline = Color(0xFF757575)
    val OutlineVariant = Color(0xFFBDBDBD)
    
    // Surface tints
    val SurfaceTint = Primary
    val InverseSurface = Color(0xFF303030)
    val InverseOnSurface = Color(0xFFECECEC)
    val InversePrimary = Color(0xFF81C784)
    val SurfaceBright = Color(0xFFFFFFFF)
    val SurfaceContainer = Color(0xFFEEEEEE)
    val SurfaceContainerHigh = Color(0xFFE0E0E0)
    val SurfaceContainerHighest = Color(0xFFD4D4D4)
    val SurfaceContainerLow = Color(0xFFF8F8F8)
    val SurfaceContainerLowest = Color(0xFFFFFFFF)
    val SurfaceDim = Color(0xFFDEDEDE)
    
    // Feedback colors for swing analysis
    val SwingPhaseSetup = Color(0xFF1976D2)
    val SwingPhaseAddress = Color(0xFF388E3C)
    val SwingPhaseTakeaway = Color(0xFFFF8F00)
    val SwingPhaseBackswing = Color(0xFFD32F2F)
    val SwingPhaseTransition = Color(0xFF7B1FA2)
    val SwingPhaseDownswing = Color(0xFF0097A7)
    val SwingPhaseImpact = Color(0xFFF57C00)
    val SwingPhaseFollowThrough = Color(0xFF5E35B1)
    val SwingPhaseFinish = Color(0xFF616161)
    
    // Feedback severity colors
    val FeedbackPositive = Color(0xFF4CAF50)
    val FeedbackInfo = Color(0xFF2196F3)
    val FeedbackWarning = Color(0xFFFF9800)
    val FeedbackCritical = Color(0xFFF44336)
    
    // Score colors
    val ScoreExcellent = Color(0xFF4CAF50)
    val ScoreGood = Color(0xFF8BC34A)
    val ScoreFair = Color(0xFFFF9800)
    val ScorePoor = Color(0xFFF44336)
}

// Dark theme colors
object GolfDarkColors {
    val Primary = Color(0xFF81C784)
    val OnPrimary = Color(0xFF000000)
    val PrimaryContainer = Color(0xFF2E7D32)
    val OnPrimaryContainer = Color(0xFFFFFFFF)
    
    val Secondary = Color(0xFFBCAAA4)
    val OnSecondary = Color(0xFF000000)
    val SecondaryContainer = Color(0xFF5D4037)
    val OnSecondaryContainer = Color(0xFFFFFFFF)
    
    val Tertiary = Color(0xFF81D4FA)
    val OnTertiary = Color(0xFF000000)
    val TertiaryContainer = Color(0xFF0277BD)
    val OnTertiaryContainer = Color(0xFFFFFFFF)
    
    val Surface = Color(0xFF121212)
    val OnSurface = Color(0xFFE0E0E0)
    val SurfaceVariant = Color(0xFF2C2C2C)
    val OnSurfaceVariant = Color(0xFFBDBDBD)
    
    val Background = Color(0xFF0D0D0D)
    val OnBackground = Color(0xFFE0E0E0)
    
    val Error = Color(0xFFEF5350)
    val OnError = Color(0xFF000000)
    val ErrorContainer = Color(0xFFB71C1C)
    val OnErrorContainer = Color(0xFFFFFFFF)
    
    val Outline = Color(0xFF757575)
    val OutlineVariant = Color(0xFF424242)
    
    val SurfaceTint = Primary
    val InverseSurface = Color(0xFFE0E0E0)
    val InverseOnSurface = Color(0xFF303030)
    val InversePrimary = Color(0xFF2E7D32)
    val SurfaceBright = Color(0xFF3A3A3A)
    val SurfaceContainer = Color(0xFF1E1E1E)
    val SurfaceContainerHigh = Color(0xFF292929)
    val SurfaceContainerHighest = Color(0xFF333333)
    val SurfaceContainerLow = Color(0xFF1A1A1A)
    val SurfaceContainerLowest = Color(0xFF0F0F0F)
    val SurfaceDim = Color(0xFF121212)
}

val lightColorScheme = lightColorScheme(
    primary = GolfColors.Primary,
    onPrimary = GolfColors.OnPrimary,
    primaryContainer = GolfColors.PrimaryContainer,
    onPrimaryContainer = GolfColors.OnPrimaryContainer,
    secondary = GolfColors.Secondary,
    onSecondary = GolfColors.OnSecondary,
    secondaryContainer = GolfColors.SecondaryContainer,
    onSecondaryContainer = GolfColors.OnSecondaryContainer,
    tertiary = GolfColors.Tertiary,
    onTertiary = GolfColors.OnTertiary,
    tertiaryContainer = GolfColors.TertiaryContainer,
    onTertiaryContainer = GolfColors.OnTertiaryContainer,
    error = GolfColors.Error,
    onError = GolfColors.OnError,
    errorContainer = GolfColors.ErrorContainer,
    onErrorContainer = GolfColors.OnErrorContainer,
    background = GolfColors.Background,
    onBackground = GolfColors.OnBackground,
    surface = GolfColors.Surface,
    onSurface = GolfColors.OnSurface,
    surfaceVariant = GolfColors.SurfaceVariant,
    onSurfaceVariant = GolfColors.OnSurfaceVariant,
    outline = GolfColors.Outline,
    outlineVariant = GolfColors.OutlineVariant,
    scrim = Color.Black,
    surfaceTint = GolfColors.SurfaceTint,
    inverseSurface = GolfColors.InverseSurface,
    inverseOnSurface = GolfColors.InverseOnSurface,
    inversePrimary = GolfColors.InversePrimary,
    surfaceBright = GolfColors.SurfaceBright,
    surfaceContainer = GolfColors.SurfaceContainer,
    surfaceContainerHigh = GolfColors.SurfaceContainerHigh,
    surfaceContainerHighest = GolfColors.SurfaceContainerHighest,
    surfaceContainerLow = GolfColors.SurfaceContainerLow,
    surfaceContainerLowest = GolfColors.SurfaceContainerLowest,
    surfaceDim = GolfColors.SurfaceDim
)

val darkColorScheme = darkColorScheme(
    primary = GolfDarkColors.Primary,
    onPrimary = GolfDarkColors.OnPrimary,
    primaryContainer = GolfDarkColors.PrimaryContainer,
    onPrimaryContainer = GolfDarkColors.OnPrimaryContainer,
    secondary = GolfDarkColors.Secondary,
    onSecondary = GolfDarkColors.OnSecondary,
    secondaryContainer = GolfDarkColors.SecondaryContainer,
    onSecondaryContainer = GolfDarkColors.OnSecondaryContainer,
    tertiary = GolfDarkColors.Tertiary,
    onTertiary = GolfDarkColors.OnTertiary,
    tertiaryContainer = GolfDarkColors.TertiaryContainer,
    onTertiaryContainer = GolfDarkColors.OnTertiaryContainer,
    error = GolfDarkColors.Error,
    onError = GolfDarkColors.OnError,
    errorContainer = GolfDarkColors.ErrorContainer,
    onErrorContainer = GolfDarkColors.OnErrorContainer,
    background = GolfDarkColors.Background,
    onBackground = GolfDarkColors.OnBackground,
    surface = GolfDarkColors.Surface,
    onSurface = GolfDarkColors.OnSurface,
    surfaceVariant = GolfDarkColors.SurfaceVariant,
    onSurfaceVariant = GolfDarkColors.OnSurfaceVariant,
    outline = GolfDarkColors.Outline,
    outlineVariant = GolfDarkColors.OutlineVariant,
    scrim = Color.Black,
    surfaceTint = GolfDarkColors.SurfaceTint,
    inverseSurface = GolfDarkColors.InverseSurface,
    inverseOnSurface = GolfDarkColors.InverseOnSurface,
    inversePrimary = GolfDarkColors.InversePrimary,
    surfaceBright = GolfDarkColors.SurfaceBright,
    surfaceContainer = GolfDarkColors.SurfaceContainer,
    surfaceContainerHigh = GolfDarkColors.SurfaceContainerHigh,
    surfaceContainerHighest = GolfDarkColors.SurfaceContainerHighest,
    surfaceContainerLow = GolfDarkColors.SurfaceContainerLow,
    surfaceContainerLowest = GolfDarkColors.SurfaceContainerLowest,
    surfaceDim = GolfDarkColors.SurfaceDim
)