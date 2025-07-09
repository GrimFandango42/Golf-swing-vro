package com.golfswing.vro.pixel.ui.accessibility

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.golfswing.vro.pixel.coaching.RealTimeCoachingEngine
import com.golfswing.vro.pixel.pose.GolfSwingPoseDetector.SwingPhase
import com.golfswing.vro.pixel.ui.theme.GolfThemeUtils

object AccessibilityUtils {
    
    // Minimum touch target size for accessibility
    val MinimumTouchTargetSize = GolfThemeUtils.MinimumTouchTargetSize.dp
    
    // WCAG AA contrast ratio requirement
    const val WCAG_AA_CONTRAST_RATIO = 4.5f
    
    // Content descriptions for golf-specific UI elements
    object ContentDescriptions {
        const val CAMERA_PREVIEW = "Camera view showing your golf swing"
        const val RECORD_BUTTON = "Record your golf swing"
        const val STOP_BUTTON = "Stop recording your golf swing"
        const val RESET_SESSION = "Reset current practice session"
        const val ANALYSIS_BUTTON = "View swing analysis"
        const val PRACTICE_MODE_SELECTOR = "Select practice mode"
        const val SWING_PHASE_INDICATOR = "Current swing phase indicator"
        const val COACHING_FEEDBACK = "Real-time coaching feedback"
        const val SCORE_DISPLAY = "Swing score display"
        const val SESSION_STATS = "Practice session statistics"
        const val QUICK_ACTION_PAUSE = "Quick action: Pause practice"
        const val QUICK_ACTION_RESUME = "Quick action: Resume practice"
        const val QUICK_ACTION_REVIEW = "Quick action: Review last swing"
        const val QUICK_ACTION_TIPS = "Quick action: Get tips"
        
        // Practice mode descriptions
        const val PRACTICE_MODE_FULL_SWING = "Full swing practice mode"
        const val PRACTICE_MODE_PUTTING = "Putting practice mode"
        const val PRACTICE_MODE_CHIPPING = "Chipping practice mode"
        const val PRACTICE_MODE_DRIVING = "Driving practice mode"
        const val PRACTICE_MODE_IRON_PLAY = "Iron play practice mode"
        const val PRACTICE_MODE_WEDGE_PLAY = "Wedge play practice mode"
    }
    
    // Semantic descriptions for swing phases
    fun getSwingPhaseDescription(phase: SwingPhase): String {
        return when (phase) {
            SwingPhase.SETUP -> "Setup phase: Preparing for the swing"
            SwingPhase.ADDRESS -> "Address phase: Addressing the ball"
            SwingPhase.TAKEAWAY -> "Takeaway phase: Starting the backswing"
            SwingPhase.BACKSWING -> "Backswing phase: Moving club back"
            SwingPhase.TRANSITION -> "Transition phase: Changing direction"
            SwingPhase.DOWNSWING -> "Downswing phase: Moving club down"
            SwingPhase.IMPACT -> "Impact phase: Striking the ball"
            SwingPhase.FOLLOW_THROUGH -> "Follow-through phase: Completing the swing"
            SwingPhase.FINISH -> "Finish phase: Swing completed"
        }
    }
    
    // Semantic descriptions for feedback severity
    fun getFeedbackSeverityDescription(severity: RealTimeCoachingEngine.FeedbackSeverity?): String {
        return when (severity) {
            RealTimeCoachingEngine.FeedbackSeverity.POSITIVE -> "Positive feedback"
            RealTimeCoachingEngine.FeedbackSeverity.INFO -> "Information"
            RealTimeCoachingEngine.FeedbackSeverity.WARNING -> "Warning"
            RealTimeCoachingEngine.FeedbackSeverity.CRITICAL -> "Critical issue"
            null -> "No feedback"
        }
    }
    
    // Score description for accessibility
    fun getScoreDescription(score: Float): String {
        return when {
            score >= 8f -> "Excellent swing, score ${String.format("%.1f", score)} out of 10"
            score >= 6f -> "Good swing, score ${String.format("%.1f", score)} out of 10"
            score >= 4f -> "Fair swing, score ${String.format("%.1f", score)} out of 10"
            else -> "Poor swing, score ${String.format("%.1f", score)} out of 10, needs improvement"
        }
    }
    
    // Session statistics description
    fun getSessionStatsDescription(stats: RealTimeCoachingEngine.SwingSession): String {
        return "Practice session: ${stats.swingCount} swings completed, " +
                "${String.format("%.1f", stats.consistencyScore * 100)}% consistency score"
    }
    
    // Check if colors meet WCAG AA contrast requirements
    fun hasGoodContrast(foreground: Color, background: Color): Boolean {
        val foregroundLuminance = getLuminance(foreground)
        val backgroundLuminance = getLuminance(background)
        
        val lighter = maxOf(foregroundLuminance, backgroundLuminance)
        val darker = minOf(foregroundLuminance, backgroundLuminance)
        
        val contrastRatio = (lighter + 0.05f) / (darker + 0.05f)
        return contrastRatio >= WCAG_AA_CONTRAST_RATIO
    }
    
    // Calculate relative luminance of a color
    private fun getLuminance(color: Color): Float {
        val r = if (color.red <= 0.03928f) color.red / 12.92f else kotlin.math.pow((color.red + 0.055f) / 1.055f, 2.4f).toFloat()
        val g = if (color.green <= 0.03928f) color.green / 12.92f else kotlin.math.pow((color.green + 0.055f) / 1.055f, 2.4f).toFloat()
        val b = if (color.blue <= 0.03928f) color.blue / 12.92f else kotlin.math.pow((color.blue + 0.055f) / 1.055f, 2.4f).toFloat()
        
        return 0.2126f * r + 0.7152f * g + 0.0722f * b
    }
}

// Extension functions for easier accessibility implementation
@Composable
fun Modifier.accessibleButton(
    contentDescription: String,
    enabled: Boolean = true
): Modifier = this
    .size(AccessibilityUtils.MinimumTouchTargetSize)
    .semantics {
        this.contentDescription = contentDescription
    }

@Composable
fun Modifier.accessibleCard(
    contentDescription: String
): Modifier = this
    .semantics {
        this.contentDescription = contentDescription
    }

@Composable
fun Modifier.accessibleText(
    contentDescription: String? = null
): Modifier = this.let { modifier ->
    if (contentDescription != null) {
        modifier.semantics {
            this.contentDescription = contentDescription
        }
    } else {
        modifier
    }
}

@Composable
fun Modifier.minimumTouchTarget(): Modifier = this
    .size(AccessibilityUtils.MinimumTouchTargetSize)
    .padding(8.dp)

// Accessibility-friendly spacing
object AccessibleSpacing {
    val Small = 8.dp
    val Medium = 16.dp
    val Large = 24.dp
    val ExtraLarge = 32.dp
    val TouchTargetPadding = 8.dp
}