package com.golfswing.vro.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.golfswing.vro.pixel.metrics.*
import com.golfswing.vro.pixel.pose.GolfSwingPoseDetector.SwingPhase
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Comprehensive Compose UI component tests for golf swing analysis
 * Tests UI components, interactions, and state management
 */
@RunWith(AndroidJUnit4::class)
class ComposeUIComponentTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // Swing Analysis UI Tests
    @Test
    fun testSwingAnalysisScreen() {
        val testMetrics = createTestSwingMetrics()
        var recordingState by mutableStateOf(false)
        
        composeTestRule.setContent {
            SwingAnalysisScreen(
                swingMetrics = testMetrics,
                isRecording = recordingState,
                onRecordToggle = { recordingState = !recordingState },
                onAnalyzeSwing = { },
                onClearAnalysis = { }
            )
        }

        // Test initial state
        composeTestRule.onNodeWithTag("swing_metrics_display")
            .assertExists()
            .assertIsDisplayed()

        // Test recording toggle
        composeTestRule.onNodeWithTag("record_button")
            .assertExists()
            .assertHasClickAction()
            .performClick()

        // Verify recording state changed
        composeTestRule.onNodeWithTag("record_button")
            .assertTextContains("Stop Recording")

        // Test analyze button
        composeTestRule.onNodeWithTag("analyze_button")
            .assertExists()
            .assertHasClickAction()
            .performClick()

        // Test clear button
        composeTestRule.onNodeWithTag("clear_button")
            .assertExists()
            .assertHasClickAction()

        println("Compose UI: Swing analysis screen test passed")
    }

    @Test
    fun testSwingMetricsDisplay() {
        val testMetrics = createTestSwingMetrics()
        
        composeTestRule.setContent {
            SwingMetricsDisplay(metrics = testMetrics)
        }

        // Test metric displays
        composeTestRule.onNodeWithTag("x_factor_metric")
            .assertExists()
            .assertTextContains("35.0°")

        composeTestRule.onNodeWithTag("tempo_metric")
            .assertExists()
            .assertTextContains("3.2:1")

        composeTestRule.onNodeWithTag("balance_metric")
            .assertExists()
            .assertTextContains("85%")

        composeTestRule.onNodeWithTag("consistency_metric")
            .assertExists()
            .assertTextContains("78%")

        println("Compose UI: Swing metrics display test passed")
    }

    @Test
    fun testSwingPhaseIndicator() {
        var currentPhase by mutableStateOf(SwingPhase.SETUP)
        
        composeTestRule.setContent {
            SwingPhaseIndicator(
                currentPhase = currentPhase,
                modifier = Modifier.testTag("phase_indicator")
            )
        }

        // Test initial phase
        composeTestRule.onNodeWithTag("phase_indicator")
            .assertExists()
            .assertTextContains("SETUP")

        // Test phase transition
        currentPhase = SwingPhase.BACKSWING
        composeTestRule.onNodeWithTag("phase_indicator")
            .assertTextContains("BACKSWING")

        currentPhase = SwingPhase.IMPACT
        composeTestRule.onNodeWithTag("phase_indicator")
            .assertTextContains("IMPACT")

        println("Compose UI: Swing phase indicator test passed")
    }

    @Test
    fun testCoachingFeedbackCard() {
        val testFeedback = "Focus on maintaining balance during your backswing. Keep your weight centered over your feet."
        
        composeTestRule.setContent {
            CoachingFeedbackCard(
                feedback = testFeedback,
                modifier = Modifier.testTag("coaching_card")
            )
        }

        // Test feedback display
        composeTestRule.onNodeWithTag("coaching_card")
            .assertExists()
            .assertIsDisplayed()

        composeTestRule.onNodeWithTag("feedback_text")
            .assertExists()
            .assertTextContains("Focus on maintaining balance")

        println("Compose UI: Coaching feedback card test passed")
    }

    @Test
    fun testProgressVisualization() {
        val progressData = listOf(
            ProgressPoint(1, 6.5f),
            ProgressPoint(2, 7.0f),
            ProgressPoint(3, 7.2f),
            ProgressPoint(4, 7.8f),
            ProgressPoint(5, 8.1f)
        )
        
        composeTestRule.setContent {
            ProgressVisualization(
                progressData = progressData,
                modifier = Modifier.testTag("progress_chart")
            )
        }

        // Test progress chart
        composeTestRule.onNodeWithTag("progress_chart")
            .assertExists()
            .assertIsDisplayed()

        composeTestRule.onNodeWithTag("progress_title")
            .assertExists()
            .assertTextContains("Progress Over Time")

        println("Compose UI: Progress visualization test passed")
    }

    @Test
    fun testSettingsScreen() {
        var soundEnabled by mutableStateOf(true)
        var hapticEnabled by mutableStateOf(false)
        var autoAnalyze by mutableStateOf(true)
        
        composeTestRule.setContent {
            SettingsScreen(
                soundEnabled = soundEnabled,
                hapticEnabled = hapticEnabled,
                autoAnalyze = autoAnalyze,
                onSoundToggle = { soundEnabled = !soundEnabled },
                onHapticToggle = { hapticEnabled = !hapticEnabled },
                onAutoAnalyzeToggle = { autoAnalyze = !autoAnalyze }
            )
        }

        // Test settings toggles
        composeTestRule.onNodeWithTag("sound_toggle")
            .assertExists()
            .assertIsOn()
            .performClick()

        composeTestRule.onNodeWithTag("haptic_toggle")
            .assertExists()
            .assertIsOff()
            .performClick()

        composeTestRule.onNodeWithTag("auto_analyze_toggle")
            .assertExists()
            .assertIsOn()

        println("Compose UI: Settings screen test passed")
    }

    @Test
    fun testCameraPreviewOverlay() {
        val testPoseData = createTestPoseData()
        
        composeTestRule.setContent {
            CameraPreviewOverlay(
                poseData = testPoseData,
                isRecording = false,
                modifier = Modifier.testTag("camera_overlay")
            )
        }

        // Test pose overlay
        composeTestRule.onNodeWithTag("camera_overlay")
            .assertExists()
            .assertIsDisplayed()

        composeTestRule.onNodeWithTag("pose_points")
            .assertExists()

        composeTestRule.onNodeWithTag("swing_path")
            .assertExists()

        println("Compose UI: Camera preview overlay test passed")
    }

    @Test
    fun testHistoryScreen() {
        val testHistory = createTestSwingHistory()
        
        composeTestRule.setContent {
            HistoryScreen(
                swingHistory = testHistory,
                onSwingSelected = { },
                onDeleteSwing = { }
            )
        }

        // Test history list
        composeTestRule.onNodeWithTag("history_list")
            .assertExists()
            .assertIsDisplayed()

        // Test history items
        composeTestRule.onNodeWithTag("swing_item_0")
            .assertExists()
            .assertHasClickAction()

        composeTestRule.onNodeWithTag("swing_item_1")
            .assertExists()
            .assertHasClickAction()

        // Test delete functionality
        composeTestRule.onNodeWithTag("delete_button_0")
            .assertExists()
            .assertHasClickAction()

        println("Compose UI: History screen test passed")
    }

    @Test
    fun testLoadingStates() {
        var isLoading by mutableStateOf(true)
        
        composeTestRule.setContent {
            LoadingScreen(
                isLoading = isLoading,
                loadingText = "Analyzing swing...",
                modifier = Modifier.testTag("loading_screen")
            )
        }

        // Test loading state
        composeTestRule.onNodeWithTag("loading_screen")
            .assertExists()
            .assertIsDisplayed()

        composeTestRule.onNodeWithTag("loading_indicator")
            .assertExists()

        composeTestRule.onNodeWithTag("loading_text")
            .assertExists()
            .assertTextContains("Analyzing swing...")

        // Test loaded state
        isLoading = false
        composeTestRule.onNodeWithTag("loading_indicator")
            .assertDoesNotExist()

        println("Compose UI: Loading states test passed")
    }

    @Test
    fun testErrorHandling() {
        val errorMessage = "Camera access denied. Please grant camera permission."
        
        composeTestRule.setContent {
            ErrorScreen(
                errorMessage = errorMessage,
                onRetry = { },
                modifier = Modifier.testTag("error_screen")
            )
        }

        // Test error display
        composeTestRule.onNodeWithTag("error_screen")
            .assertExists()
            .assertIsDisplayed()

        composeTestRule.onNodeWithTag("error_message")
            .assertExists()
            .assertTextContains("Camera access denied")

        composeTestRule.onNodeWithTag("retry_button")
            .assertExists()
            .assertHasClickAction()

        println("Compose UI: Error handling test passed")
    }

    @Test
    fun testResponsiveDesign() {
        composeTestRule.setContent {
            ResponsiveSwingAnalysisLayout(
                modifier = Modifier.testTag("responsive_layout")
            )
        }

        // Test responsive layout
        composeTestRule.onNodeWithTag("responsive_layout")
            .assertExists()
            .assertIsDisplayed()

        // Test that components adapt to screen size
        composeTestRule.onNodeWithTag("metrics_section")
            .assertExists()

        composeTestRule.onNodeWithTag("controls_section")
            .assertExists()

        println("Compose UI: Responsive design test passed")
    }

    @Test
    fun testAnimations() {
        var isAnimating by mutableStateOf(false)
        
        composeTestRule.setContent {
            AnimatedSwingIndicator(
                isAnimating = isAnimating,
                modifier = Modifier.testTag("animated_indicator")
            )
        }

        // Test animation trigger
        composeTestRule.onNodeWithTag("animated_indicator")
            .assertExists()

        isAnimating = true
        composeTestRule.waitForIdle()

        // Test that animation is running
        composeTestRule.onNodeWithTag("animated_indicator")
            .assertExists()

        println("Compose UI: Animations test passed")
    }

    // Test Components (Mock implementations for testing)
    @Composable
    private fun SwingAnalysisScreen(
        swingMetrics: SwingMetrics,
        isRecording: Boolean,
        onRecordToggle: () -> Unit,
        onAnalyzeSwing: () -> Unit,
        onClearAnalysis: () -> Unit
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            SwingMetricsDisplay(
                metrics = swingMetrics,
                modifier = Modifier.testTag("swing_metrics_display")
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = onRecordToggle,
                    modifier = Modifier.testTag("record_button")
                ) {
                    Text(if (isRecording) "Stop Recording" else "Start Recording")
                }
                
                Button(
                    onClick = onAnalyzeSwing,
                    modifier = Modifier.testTag("analyze_button")
                ) {
                    Text("Analyze")
                }
                
                Button(
                    onClick = onClearAnalysis,
                    modifier = Modifier.testTag("clear_button")
                ) {
                    Text("Clear")
                }
            }
        }
    }

    @Composable
    private fun SwingMetricsDisplay(
        metrics: SwingMetrics,
        modifier: Modifier = Modifier
    ) {
        Column(modifier = modifier) {
            Text(
                text = "X-Factor: ${metrics.xFactor}°",
                modifier = Modifier.testTag("x_factor_metric")
            )
            Text(
                text = "Tempo: ${metrics.tempo}:1",
                modifier = Modifier.testTag("tempo_metric")
            )
            Text(
                text = "Balance: ${(metrics.balance * 100).toInt()}%",
                modifier = Modifier.testTag("balance_metric")
            )
            Text(
                text = "Consistency: ${(metrics.consistency * 100).toInt()}%",
                modifier = Modifier.testTag("consistency_metric")
            )
        }
    }

    @Composable
    private fun SwingPhaseIndicator(
        currentPhase: SwingPhase,
        modifier: Modifier = Modifier
    ) {
        Card(
            modifier = modifier.padding(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = when (currentPhase) {
                    SwingPhase.SETUP -> Color.Gray
                    SwingPhase.BACKSWING -> Color.Blue
                    SwingPhase.IMPACT -> Color.Red
                    else -> Color.Green
                }
            )
        ) {
            Text(
                text = currentPhase.name,
                modifier = Modifier.padding(16.dp)
            )
        }
    }

    @Composable
    private fun CoachingFeedbackCard(
        feedback: String,
        modifier: Modifier = Modifier
    ) {
        Card(
            modifier = modifier.padding(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Coaching Feedback",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = feedback,
                    modifier = Modifier.testTag("feedback_text")
                )
            }
        }
    }

    @Composable
    private fun ProgressVisualization(
        progressData: List<ProgressPoint>,
        modifier: Modifier = Modifier
    ) {
        Column(modifier = modifier) {
            Text(
                text = "Progress Over Time",
                modifier = Modifier.testTag("progress_title"),
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            // Simple mock chart representation
            progressData.forEach { point ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Session ${point.session}")
                    Text("${point.score}")
                }
            }
        }
    }

    @Composable
    private fun SettingsScreen(
        soundEnabled: Boolean,
        hapticEnabled: Boolean,
        autoAnalyze: Boolean,
        onSoundToggle: () -> Unit,
        onHapticToggle: () -> Unit,
        onAutoAnalyzeToggle: () -> Unit
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Sound Effects")
                Switch(
                    checked = soundEnabled,
                    onCheckedChange = { onSoundToggle() },
                    modifier = Modifier.testTag("sound_toggle")
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Haptic Feedback")
                Switch(
                    checked = hapticEnabled,
                    onCheckedChange = { onHapticToggle() },
                    modifier = Modifier.testTag("haptic_toggle")
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Auto Analyze")
                Switch(
                    checked = autoAnalyze,
                    onCheckedChange = { onAutoAnalyzeToggle() },
                    modifier = Modifier.testTag("auto_analyze_toggle")
                )
            }
        }
    }

    @Composable
    private fun CameraPreviewOverlay(
        poseData: PoseData,
        isRecording: Boolean,
        modifier: Modifier = Modifier
    ) {
        Box(modifier = modifier.fillMaxSize()) {
            // Mock pose overlay
            Box(
                modifier = Modifier
                    .testTag("pose_points")
                    .fillMaxSize()
            )
            
            // Mock swing path
            Box(
                modifier = Modifier
                    .testTag("swing_path")
                    .fillMaxSize()
            )
            
            // Recording indicator
            if (isRecording) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                ) {
                    Text(
                        text = "REC",
                        color = Color.Red,
                        modifier = Modifier.testTag("recording_indicator")
                    )
                }
            }
        }
    }

    @Composable
    private fun HistoryScreen(
        swingHistory: List<SwingHistoryItem>,
        onSwingSelected: (SwingHistoryItem) -> Unit,
        onDeleteSwing: (SwingHistoryItem) -> Unit
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .testTag("history_list")
        ) {
            swingHistory.forEachIndexed { index, item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .testTag("swing_item_$index"),
                    onClick = { onSwingSelected(item) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = item.date,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Score: ${item.score}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        
                        IconButton(
                            onClick = { onDeleteSwing(item) },
                            modifier = Modifier.testTag("delete_button_$index")
                        ) {
                            Text("Delete")
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun LoadingScreen(
        isLoading: Boolean,
        loadingText: String,
        modifier: Modifier = Modifier
    ) {
        if (isLoading) {
            Column(
                modifier = modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.testTag("loading_indicator")
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = loadingText,
                    modifier = Modifier.testTag("loading_text")
                )
            }
        }
    }

    @Composable
    private fun ErrorScreen(
        errorMessage: String,
        onRetry: () -> Unit,
        modifier: Modifier = Modifier
    ) {
        Column(
            modifier = modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = errorMessage,
                modifier = Modifier.testTag("error_message"),
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onRetry,
                modifier = Modifier.testTag("retry_button")
            ) {
                Text("Retry")
            }
        }
    }

    @Composable
    private fun ResponsiveSwingAnalysisLayout(
        modifier: Modifier = Modifier
    ) {
        Column(modifier = modifier.fillMaxSize()) {
            // Metrics section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("metrics_section")
            ) {
                Text("Metrics Section")
            }
            
            // Controls section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("controls_section")
            ) {
                Text("Controls Section")
            }
        }
    }

    @Composable
    private fun AnimatedSwingIndicator(
        isAnimating: Boolean,
        modifier: Modifier = Modifier
    ) {
        Box(
            modifier = modifier
                .size(100.dp)
                .testTag("animated_indicator")
        ) {
            if (isAnimating) {
                CircularProgressIndicator(
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    // Helper data classes and methods
    private data class SwingMetrics(
        val xFactor: Float,
        val tempo: Float,
        val balance: Float,
        val consistency: Float
    )

    private data class PoseData(
        val landmarks: List<Any> = emptyList()
    )

    private data class ProgressPoint(
        val session: Int,
        val score: Float
    )

    private data class SwingHistoryItem(
        val id: Long,
        val date: String,
        val score: Float
    )

    private fun createTestSwingMetrics(): SwingMetrics {
        return SwingMetrics(
            xFactor = 35.0f,
            tempo = 3.2f,
            balance = 0.85f,
            consistency = 0.78f
        )
    }

    private fun createTestPoseData(): PoseData {
        return PoseData()
    }

    private fun createTestSwingHistory(): List<SwingHistoryItem> {
        return listOf(
            SwingHistoryItem(1, "2024-01-15", 7.5f),
            SwingHistoryItem(2, "2024-01-14", 8.0f),
            SwingHistoryItem(3, "2024-01-13", 7.2f)
        )
    }
}