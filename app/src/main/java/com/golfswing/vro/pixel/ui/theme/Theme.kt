package com.golfswing.vro.pixel.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
fun GolfSwingTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        // Dynamic color is available on Android 12+
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        
        // Use custom dark theme
        darkTheme -> darkColorScheme
        
        // Use custom light theme
        else -> lightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = GolfTypography,
        content = content
    )
}

// Extension functions for easier theme access
@Composable
fun getSwingPhaseColor(phase: String): androidx.compose.ui.graphics.Color {
    return when (phase) {
        "SETUP" -> GolfColors.SwingPhaseSetup
        "ADDRESS" -> GolfColors.SwingPhaseAddress
        "TAKEAWAY" -> GolfColors.SwingPhaseTakeaway
        "BACKSWING" -> GolfColors.SwingPhaseBackswing
        "TRANSITION" -> GolfColors.SwingPhaseTransition
        "DOWNSWING" -> GolfColors.SwingPhaseDownswing
        "IMPACT" -> GolfColors.SwingPhaseImpact
        "FOLLOW_THROUGH" -> GolfColors.SwingPhaseFollowThrough
        "FINISH" -> GolfColors.SwingPhaseFinish
        else -> MaterialTheme.colorScheme.outline
    }
}

@Composable
fun getFeedbackSeverityColor(severity: String?): androidx.compose.ui.graphics.Color {
    return when (severity) {
        "POSITIVE" -> GolfColors.FeedbackPositive
        "INFO" -> GolfColors.FeedbackInfo
        "WARNING" -> GolfColors.FeedbackWarning
        "CRITICAL" -> GolfColors.FeedbackCritical
        else -> MaterialTheme.colorScheme.onSurface
    }
}

@Composable
fun getScoreColor(score: Float): androidx.compose.ui.graphics.Color {
    return when {
        score >= 8f -> GolfColors.ScoreExcellent
        score >= 6f -> GolfColors.ScoreGood
        score >= 4f -> GolfColors.ScoreFair
        else -> GolfColors.ScorePoor
    }
}

// Theme utilities for consistent spacing and shapes
object GolfThemeUtils {
    // Spacing scale
    val SpacingTiny = 4
    val SpacingSmall = 8
    val SpacingMedium = 16
    val SpacingLarge = 24
    val SpacingExtraLarge = 32
    val SpacingHuge = 48
    
    // Common corner radius values
    val CornerRadiusSmall = 8
    val CornerRadiusMedium = 12
    val CornerRadiusLarge = 16
    val CornerRadiusExtraLarge = 24
    
    // Elevation values
    val ElevationNone = 0
    val ElevationSmall = 2
    val ElevationMedium = 4
    val ElevationLarge = 8
    val ElevationExtraLarge = 12
    
    // Animation duration values
    val AnimationDurationShort = 200
    val AnimationDurationMedium = 300
    val AnimationDurationLong = 500
    
    // Minimum touch target size for accessibility
    val MinimumTouchTargetSize = 48
}