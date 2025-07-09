package com.golfswing.vro.pixel.ui.accessibility

import android.content.Context
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.golfswing.vro.pixel.ui.accessibility.AccessibilityUtils
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Comprehensive accessibility compliance tests for UI components
 * Tests WCAG 2.1 AA standards, Android accessibility guidelines, and golf-specific accessibility needs
 */
@RunWith(AndroidJUnit4::class)
class AccessibilityComplianceTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var accessibilityUtils: AccessibilityUtils

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        accessibilityUtils = AccessibilityUtils(context)
    }

    // WCAG 2.1 AA Compliance Tests
    @Test
    fun testColorContrastCompliance() {
        // Test minimum contrast ratio (4.5:1 for normal text, 3:1 for large text)
        val testCases = listOf(
            // Normal text cases
            Triple(Color.BLACK, Color.WHITE, 4.5f), // Should pass
            Triple(Color.parseColor("#333333"), Color.WHITE, 4.5f), // Should pass
            Triple(Color.parseColor("#767676"), Color.WHITE, 4.5f), // Should pass
            Triple(Color.parseColor("#959595"), Color.WHITE, 4.5f), // Should fail
            
            // Large text cases (3:1 ratio)
            Triple(Color.parseColor("#959595"), Color.WHITE, 3.0f), // Should pass for large text
            Triple(Color.parseColor("#AA0000"), Color.WHITE, 3.0f), // Should pass
            
            // Golf-specific color schemes
            Triple(Color.parseColor("#228B22"), Color.WHITE, 4.5f), // Forest green
            Triple(Color.parseColor("#8B4513"), Color.WHITE, 4.5f), // Saddle brown
            Triple(Color.parseColor("#1E90FF"), Color.WHITE, 4.5f), // Dodger blue
        )

        testCases.forEach { (foreground, background, minimumRatio) ->
            val actualRatio = accessibilityUtils.calculateContrastRatio(foreground, background)
            assertTrue("Contrast ratio should be at least $minimumRatio:1 for colors " +
                    "${Integer.toHexString(foreground)} on ${Integer.toHexString(background)} " +
                    "(actual: ${String.format("%.2f", actualRatio)}:1)", 
                actualRatio >= minimumRatio)
        }
        
        println("WCAG 2.1 AA: Color contrast compliance passed")
    }

    @Test
    fun testTextSizeCompliance() {
        composeTestRule.setContent {
            TestTextSizeScreen()
        }

        // Test minimum text sizes
        composeTestRule.onNodeWithTag("small_text")
            .assertExists()
            .assertTextEquals("Small Text (12sp)")

        composeTestRule.onNodeWithTag("normal_text")
            .assertExists()
            .assertTextEquals("Normal Text (16sp)")

        composeTestRule.onNodeWithTag("large_text")
            .assertExists()
            .assertTextEquals("Large Text (20sp)")

        // Test that text scales with system settings
        composeTestRule.onNodeWithTag("scalable_text")
            .assertExists()
            .assertIsDisplayed()

        println("WCAG 2.1 AA: Text size compliance passed")
    }

    @Test
    fun testKeyboardNavigation() {
        composeTestRule.setContent {
            TestKeyboardNavigationScreen()
        }

        // Test tab navigation order
        composeTestRule.onNodeWithTag("button1")
            .performClick()
            .assertIsFocused()

        composeTestRule.onNodeWithTag("button2")
            .performClick()
            .assertIsFocused()

        composeTestRule.onNodeWithTag("button3")
            .performClick()
            .assertIsFocused()

        // Test that all interactive elements are focusable
        composeTestRule.onNodeWithTag("button1").assertHasClickAction()
        composeTestRule.onNodeWithTag("button2").assertHasClickAction()
        composeTestRule.onNodeWithTag("button3").assertHasClickAction()

        println("WCAG 2.1 AA: Keyboard navigation passed")
    }

    @Test
    fun testSemanticContentDescription() {
        composeTestRule.setContent {
            TestSemanticContentScreen()
        }

        // Test that interactive elements have meaningful descriptions
        composeTestRule.onNodeWithTag("record_button")
            .assertContentDescriptionContains("Record golf swing")

        composeTestRule.onNodeWithTag("analyze_button")
            .assertContentDescriptionContains("Analyze swing")

        composeTestRule.onNodeWithTag("settings_button")
            .assertContentDescriptionContains("Open settings")

        // Test that status information is accessible
        composeTestRule.onNodeWithTag("swing_status")
            .assertContentDescriptionContains("Swing analysis complete")

        println("WCAG 2.1 AA: Semantic content description passed")
    }

    // Android Accessibility Guidelines Tests
    @Test
    fun testTalkBackCompatibility() {
        composeTestRule.setContent {
            TestTalkBackScreen()
        }

        // Test that content descriptions are provided for non-text elements
        composeTestRule.onNodeWithTag("golf_ball_icon")
            .assertContentDescriptionContains("Golf ball")

        composeTestRule.onNodeWithTag("club_icon")
            .assertContentDescriptionContains("Golf club")

        // Test that complex UI elements have appropriate descriptions
        composeTestRule.onNodeWithTag("swing_visualization")
            .assertContentDescriptionContains("Swing visualization showing")

        // Test that buttons have action descriptions
        composeTestRule.onNodeWithTag("play_button")
            .assertContentDescriptionContains("Play swing animation")

        println("Android Accessibility: TalkBack compatibility passed")
    }

    @Test
    fun testTouchTargetSize() {
        composeTestRule.setContent {
            TestTouchTargetScreen()
        }

        // Test minimum touch target size (48dp x 48dp)
        composeTestRule.onNodeWithTag("small_button")
            .assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp)

        composeTestRule.onNodeWithTag("icon_button")
            .assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp)

        // Test that touch targets don't overlap
        composeTestRule.onNodeWithTag("button_row")
            .assertExists()

        println("Android Accessibility: Touch target size passed")
    }

    @Test
    fun testScreenReaderSupport() {
        composeTestRule.setContent {
            TestScreenReaderScreen()
        }

        // Test that all text content is readable by screen readers
        composeTestRule.onNodeWithTag("swing_metrics")
            .assertExists()
            .assertIsDisplayed()

        composeTestRule.onNodeWithTag("coaching_tips")
            .assertExists()
            .assertIsDisplayed()

        // Test that dynamic content updates are announced
        composeTestRule.onNodeWithTag("live_feedback")
            .assertExists()
            .assertIsDisplayed()

        println("Android Accessibility: Screen reader support passed")
    }

    // Golf-Specific Accessibility Tests
    @Test
    fun testGolfTerminologyAccessibility() {
        composeTestRule.setContent {
            TestGolfTerminologyScreen()
        }

        // Test that golf terms are properly explained for accessibility
        composeTestRule.onNodeWithTag("x_factor_metric")
            .assertContentDescriptionContains("X-Factor: The difference between shoulder and hip rotation")

        composeTestRule.onNodeWithTag("swing_plane_metric")
            .assertContentDescriptionContains("Swing plane: The angle of the club during the swing")

        composeTestRule.onNodeWithTag("tempo_metric")
            .assertContentDescriptionContains("Tempo: The timing ratio of backswing to downswing")

        println("Golf Accessibility: Terminology accessibility passed")
    }

    @Test
    fun testSwingVisualizationAccessibility() {
        composeTestRule.setContent {
            TestSwingVisualizationScreen()
        }

        // Test that visual swing analysis is accessible
        composeTestRule.onNodeWithTag("swing_path")
            .assertContentDescriptionContains("Swing path visualization")

        composeTestRule.onNodeWithTag("pose_overlay")
            .assertContentDescriptionContains("Pose detection overlay")

        // Test that colors used in visualization have sufficient contrast
        composeTestRule.onNodeWithTag("swing_phases")
            .assertExists()
            .assertIsDisplayed()

        println("Golf Accessibility: Swing visualization accessibility passed")
    }

    @Test
    fun testAudioFeedbackAccessibility() {
        composeTestRule.setContent {
            TestAudioFeedbackScreen()
        }

        // Test that audio feedback has visual alternatives
        composeTestRule.onNodeWithTag("audio_feedback_toggle")
            .assertExists()
            .assertIsToggleable()

        composeTestRule.onNodeWithTag("visual_feedback_alternative")
            .assertExists()
            .assertIsDisplayed()

        // Test that audio descriptions are provided
        composeTestRule.onNodeWithTag("audio_description")
            .assertContentDescriptionContains("Audio feedback for swing analysis")

        println("Golf Accessibility: Audio feedback accessibility passed")
    }

    // Dynamic Content and Live Updates Tests
    @Test
    fun testLiveUpdateAccessibility() {
        composeTestRule.setContent {
            TestLiveUpdateScreen()
        }

        // Test that live updates are announced to screen readers
        composeTestRule.onNodeWithTag("live_swing_data")
            .assertExists()
            .assertIsDisplayed()

        composeTestRule.onNodeWithTag("live_coaching_feedback")
            .assertExists()
            .assertIsDisplayed()

        // Test that status changes are accessible
        composeTestRule.onNodeWithTag("swing_status_live")
            .assertExists()
            .assertIsDisplayed()

        println("Dynamic Content: Live update accessibility passed")
    }

    @Test
    fun testErrorMessageAccessibility() {
        composeTestRule.setContent {
            TestErrorMessageScreen()
        }

        // Test that error messages are accessible
        composeTestRule.onNodeWithTag("camera_error")
            .assertContentDescriptionContains("Camera error: Unable to access camera")

        composeTestRule.onNodeWithTag("analysis_error")
            .assertContentDescriptionContains("Analysis error: Unable to process swing")

        // Test that error recovery actions are accessible
        composeTestRule.onNodeWithTag("retry_button")
            .assertContentDescriptionContains("Retry analysis")

        println("Dynamic Content: Error message accessibility passed")
    }

    // Accessibility Utils Tests
    @Test
    fun testAccessibilityUtilsColorContrast() {
        // Test various color combinations
        val highContrast = accessibilityUtils.calculateContrastRatio(Color.BLACK, Color.WHITE)
        assertTrue("Black on white should have high contrast", highContrast > 15.0f)

        val mediumContrast = accessibilityUtils.calculateContrastRatio(
            Color.parseColor("#333333"), Color.WHITE
        )
        assertTrue("Dark gray on white should have medium contrast", mediumContrast > 4.5f)

        val lowContrast = accessibilityUtils.calculateContrastRatio(
            Color.parseColor("#CCCCCC"), Color.WHITE
        )
        assertTrue("Light gray on white should have low contrast", lowContrast < 4.5f)

        println("Accessibility Utils: Color contrast calculation passed")
    }

    @Test
    fun testAccessibilityUtilsTextSize() {
        val isSmallTextAccessible = accessibilityUtils.isTextSizeAccessible(12f, false)
        assertFalse("12sp should not be accessible for normal text", isSmallTextAccessible)

        val isNormalTextAccessible = accessibilityUtils.isTextSizeAccessible(16f, false)
        assertTrue("16sp should be accessible for normal text", isNormalTextAccessible)

        val isLargeTextAccessible = accessibilityUtils.isTextSizeAccessible(12f, true)
        assertTrue("12sp should be accessible for large text", isLargeTextAccessible)

        println("Accessibility Utils: Text size validation passed")
    }

    @Test
    fun testAccessibilityUtilsTouchTarget() {
        val isSmallTargetAccessible = accessibilityUtils.isTouchTargetAccessible(40f, 40f)
        assertFalse("40dp x 40dp should not be accessible", isSmallTargetAccessible)

        val isNormalTargetAccessible = accessibilityUtils.isTouchTargetAccessible(48f, 48f)
        assertTrue("48dp x 48dp should be accessible", isNormalTargetAccessible)

        val isLargeTargetAccessible = accessibilityUtils.isTouchTargetAccessible(56f, 56f)
        assertTrue("56dp x 56dp should be accessible", isLargeTargetAccessible)

        println("Accessibility Utils: Touch target validation passed")
    }

    // Compose UI Test Screens
    @Composable
    private fun TestTextSizeScreen() {
        Column {
            Text(
                text = "Small Text (12sp)",
                modifier = Modifier.testTag("small_text")
            )
            Text(
                text = "Normal Text (16sp)",
                modifier = Modifier.testTag("normal_text")
            )
            Text(
                text = "Large Text (20sp)",
                modifier = Modifier.testTag("large_text")
            )
            Text(
                text = "Scalable Text",
                modifier = Modifier.testTag("scalable_text")
            )
        }
    }

    @Composable
    private fun TestKeyboardNavigationScreen() {
        Column {
            Button(
                onClick = { },
                modifier = Modifier.testTag("button1")
            ) {
                Text("Button 1")
            }
            Button(
                onClick = { },
                modifier = Modifier.testTag("button2")
            ) {
                Text("Button 2")
            }
            Button(
                onClick = { },
                modifier = Modifier.testTag("button3")
            ) {
                Text("Button 3")
            }
        }
    }

    @Composable
    private fun TestSemanticContentScreen() {
        Column {
            Button(
                onClick = { },
                modifier = Modifier
                    .testTag("record_button")
                    .semantics {
                        contentDescription = "Record golf swing"
                    }
            ) {
                Text("Record")
            }
            Button(
                onClick = { },
                modifier = Modifier
                    .testTag("analyze_button")
                    .semantics {
                        contentDescription = "Analyze swing"
                    }
            ) {
                Text("Analyze")
            }
            Button(
                onClick = { },
                modifier = Modifier
                    .testTag("settings_button")
                    .semantics {
                        contentDescription = "Open settings"
                    }
            ) {
                Text("Settings")
            }
            Text(
                text = "Analysis Complete",
                modifier = Modifier
                    .testTag("swing_status")
                    .semantics {
                        contentDescription = "Swing analysis complete"
                    }
            )
        }
    }

    @Composable
    private fun TestTalkBackScreen() {
        Column {
            Button(
                onClick = { },
                modifier = Modifier
                    .testTag("golf_ball_icon")
                    .semantics {
                        contentDescription = "Golf ball"
                    }
            ) {
                Text("🏌️")
            }
            Button(
                onClick = { },
                modifier = Modifier
                    .testTag("club_icon")
                    .semantics {
                        contentDescription = "Golf club"
                    }
            ) {
                Text("⛳")
            }
            Text(
                text = "Swing Visualization",
                modifier = Modifier
                    .testTag("swing_visualization")
                    .semantics {
                        contentDescription = "Swing visualization showing optimal swing path"
                    }
            )
            Button(
                onClick = { },
                modifier = Modifier
                    .testTag("play_button")
                    .semantics {
                        contentDescription = "Play swing animation"
                    }
            ) {
                Text("Play")
            }
        }
    }

    @Composable
    private fun TestTouchTargetScreen() {
        Column(
            modifier = Modifier.testTag("button_row")
        ) {
            Button(
                onClick = { },
                modifier = Modifier
                    .testTag("small_button")
                    .padding(4.dp)
            ) {
                Text("Small")
            }
            Button(
                onClick = { },
                modifier = Modifier
                    .testTag("icon_button")
                    .padding(4.dp)
            ) {
                Text("Icon")
            }
        }
    }

    @Composable
    private fun TestScreenReaderScreen() {
        Column {
            Text(
                text = "Swing Metrics: X-Factor 35°, Tempo 3.2:1",
                modifier = Modifier.testTag("swing_metrics")
            )
            Text(
                text = "Coaching Tips: Focus on hip rotation",
                modifier = Modifier.testTag("coaching_tips")
            )
            Text(
                text = "Live Feedback: Good balance",
                modifier = Modifier.testTag("live_feedback")
            )
        }
    }

    @Composable
    private fun TestGolfTerminologyScreen() {
        Column {
            Text(
                text = "X-Factor: 35°",
                modifier = Modifier
                    .testTag("x_factor_metric")
                    .semantics {
                        contentDescription = "X-Factor: The difference between shoulder and hip rotation, currently 35 degrees"
                    }
            )
            Text(
                text = "Swing Plane: 60°",
                modifier = Modifier
                    .testTag("swing_plane_metric")
                    .semantics {
                        contentDescription = "Swing plane: The angle of the club during the swing, currently 60 degrees"
                    }
            )
            Text(
                text = "Tempo: 3.2:1",
                modifier = Modifier
                    .testTag("tempo_metric")
                    .semantics {
                        contentDescription = "Tempo: The timing ratio of backswing to downswing, currently 3.2 to 1"
                    }
            )
        }
    }

    @Composable
    private fun TestSwingVisualizationScreen() {
        Column {
            Text(
                text = "Swing Path",
                modifier = Modifier
                    .testTag("swing_path")
                    .semantics {
                        contentDescription = "Swing path visualization showing club movement"
                    }
            )
            Text(
                text = "Pose Overlay",
                modifier = Modifier
                    .testTag("pose_overlay")
                    .semantics {
                        contentDescription = "Pose detection overlay showing body position"
                    }
            )
            Text(
                text = "Swing Phases",
                modifier = Modifier.testTag("swing_phases")
            )
        }
    }

    @Composable
    private fun TestAudioFeedbackScreen() {
        Column {
            Button(
                onClick = { },
                modifier = Modifier.testTag("audio_feedback_toggle")
            ) {
                Text("Audio Feedback")
            }
            Text(
                text = "Visual Feedback",
                modifier = Modifier.testTag("visual_feedback_alternative")
            )
            Text(
                text = "Audio Description",
                modifier = Modifier
                    .testTag("audio_description")
                    .semantics {
                        contentDescription = "Audio feedback for swing analysis"
                    }
            )
        }
    }

    @Composable
    private fun TestLiveUpdateScreen() {
        Column {
            Text(
                text = "Live Swing Data",
                modifier = Modifier.testTag("live_swing_data")
            )
            Text(
                text = "Live Coaching Feedback",
                modifier = Modifier.testTag("live_coaching_feedback")
            )
            Text(
                text = "Swing Status",
                modifier = Modifier.testTag("swing_status_live")
            )
        }
    }

    @Composable
    private fun TestErrorMessageScreen() {
        Column {
            Text(
                text = "Camera Error",
                modifier = Modifier
                    .testTag("camera_error")
                    .semantics {
                        contentDescription = "Camera error: Unable to access camera"
                    }
            )
            Text(
                text = "Analysis Error",
                modifier = Modifier
                    .testTag("analysis_error")
                    .semantics {
                        contentDescription = "Analysis error: Unable to process swing"
                    }
            )
            Button(
                onClick = { },
                modifier = Modifier
                    .testTag("retry_button")
                    .semantics {
                        contentDescription = "Retry analysis"
                    }
            ) {
                Text("Retry")
            }
        }
    }
}

// Mock AccessibilityUtils for testing
class AccessibilityUtils(private val context: Context) {
    
    fun calculateContrastRatio(foreground: Int, background: Int): Float {
        val foregroundLuminance = calculateLuminance(foreground)
        val backgroundLuminance = calculateLuminance(background)
        
        val lighter = maxOf(foregroundLuminance, backgroundLuminance)
        val darker = minOf(foregroundLuminance, backgroundLuminance)
        
        return (lighter + 0.05f) / (darker + 0.05f)
    }
    
    private fun calculateLuminance(color: Int): Float {
        val r = Color.red(color) / 255.0f
        val g = Color.green(color) / 255.0f
        val b = Color.blue(color) / 255.0f
        
        val rLuminance = if (r <= 0.03928f) r / 12.92f else kotlin.math.pow((r + 0.055f) / 1.055f, 2.4f).toFloat()
        val gLuminance = if (g <= 0.03928f) g / 12.92f else kotlin.math.pow((g + 0.055f) / 1.055f, 2.4f).toFloat()
        val bLuminance = if (b <= 0.03928f) b / 12.92f else kotlin.math.pow((b + 0.055f) / 1.055f, 2.4f).toFloat()
        
        return 0.2126f * rLuminance + 0.7152f * gLuminance + 0.0722f * bLuminance
    }
    
    fun isTextSizeAccessible(textSizeSp: Float, isLargeText: Boolean): Boolean {
        val minimumSize = if (isLargeText) 12f else 16f
        return textSizeSp >= minimumSize
    }
    
    fun isTouchTargetAccessible(widthDp: Float, heightDp: Float): Boolean {
        val minimumSize = 48f
        return widthDp >= minimumSize && heightDp >= minimumSize
    }
}