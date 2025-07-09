package com.golfswing.vro.pixel.ui.camera

import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.golfswing.vro.pixel.coaching.RealTimeCoachingEngine
import com.golfswing.vro.pixel.pose.GolfSwingPoseDetector.SwingPhase
import com.golfswing.vro.pixel.ui.accessibility.AccessibilityUtils
import com.golfswing.vro.pixel.ui.accessibility.accessibleButton
import com.golfswing.vro.pixel.ui.accessibility.accessibleCard
import com.golfswing.vro.pixel.ui.practice.PracticeMode
import com.golfswing.vro.pixel.ui.practice.PracticeModeSelector
import com.golfswing.vro.pixel.ui.practice.PracticeModes
import com.golfswing.vro.pixel.ui.quickactions.ContextualQuickActions
import com.golfswing.vro.pixel.ui.quickactions.PrimaryQuickActionFAB
import com.golfswing.vro.pixel.ui.quickactions.QuickAction
import com.golfswing.vro.pixel.ui.quickactions.QuickActions
import com.golfswing.vro.pixel.ui.theme.GolfTextStyles
import com.golfswing.vro.pixel.ui.theme.GolfThemeUtils
import com.golfswing.vro.pixel.ui.theme.getFeedbackSeverityColor
import com.golfswing.vro.pixel.ui.theme.getScoreColor
import com.golfswing.vro.pixel.ui.theme.getSwingPhaseColor
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.isGranted

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraScreen(
    viewModel: CameraViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    val cameraPermissionState = rememberPermissionState(
        android.Manifest.permission.CAMERA
    )
    
    val cameraState by viewModel.cameraState.collectAsState()
    val isRecording by viewModel.isRecording.collectAsState()
    val currentSwingPhase by viewModel.currentSwingPhase.collectAsState()
    val coachingFeedback by viewModel.coachingFeedback.collectAsState()
    val swingAnalysis by viewModel.swingAnalysis.collectAsState()
    val sessionStats by viewModel.sessionStats.collectAsState()
    
    // Practice mode state
    var selectedPracticeMode by remember { mutableStateOf<PracticeMode?>(PracticeModes.FULL_SWING) }
    var isPaused by remember { mutableStateOf(false) }
    var showModeSelector by remember { mutableStateOf(false) }

    if (cameraPermissionState.status.isGranted) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Camera Preview
            AndroidView(
                factory = { context ->
                    PreviewView(context).apply {
                        viewModel.initializeCamera(lifecycleOwner, this)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
            
            // Simplified overlay UI
            SimplifiedCameraOverlay(
                swingPhase = currentSwingPhase,
                coachingFeedback = coachingFeedback,
                swingAnalysis = swingAnalysis,
                sessionStats = sessionStats,
                isRecording = isRecording,
                isPaused = isPaused,
                selectedPracticeMode = selectedPracticeMode,
                showModeSelector = showModeSelector,
                onStartRecording = { viewModel.startRecording() },
                onStopRecording = { viewModel.stopRecording() },
                onPauseToggle = { isPaused = !isPaused },
                onResetSession = { viewModel.resetSession() },
                onModeSelected = { mode -> 
                    selectedPracticeMode = mode
                    showModeSelector = false
                },
                onShowModeSelector = { showModeSelector = true },
                onHideModeSelector = { showModeSelector = false },
                modifier = Modifier.fillMaxSize()
            )
        }
    } else {
        // Permission request screen
        PermissionRequestScreen(
            onRequestPermission = { cameraPermissionState.launchPermissionRequest() }
        )
    }
}

@Composable
fun SimplifiedCameraOverlay(
    swingPhase: SwingPhase,
    coachingFeedback: RealTimeCoachingEngine.CoachingFeedback?,
    swingAnalysis: RealTimeCoachingEngine.SwingAnalysis?,
    sessionStats: RealTimeCoachingEngine.SwingSession,
    isRecording: Boolean,
    isPaused: Boolean,
    selectedPracticeMode: PracticeMode?,
    showModeSelector: Boolean,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onPauseToggle: () -> Unit,
    onResetSession: () -> Unit,
    onModeSelected: (PracticeMode) -> Unit,
    onShowModeSelector: () -> Unit,
    onHideModeSelector: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        // Top: Practice mode selector (when shown)
        if (showModeSelector) {
            PracticeModeSelector(
                selectedMode = selectedPracticeMode,
                onModeSelected = onModeSelected,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(GolfThemeUtils.SpacingMedium.dp)
            )
        } else {
            // Simplified status indicator
            SimplifiedStatusIndicator(
                swingPhase = swingPhase,
                practiceMode = selectedPracticeMode,
                sessionStats = sessionStats,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(GolfThemeUtils.SpacingMedium.dp)
            )
        }
        
        // Center: Essential feedback only
        EssentialFeedbackCard(
            feedback = coachingFeedback,
            swingAnalysis = swingAnalysis,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = GolfThemeUtils.SpacingMedium.dp)
        )
        
        // Bottom right: Primary action FAB
        PrimaryQuickActionFAB(
            action = if (isRecording) {
                QuickActions.PAUSE_PRACTICE
            } else {
                QuickActions.RESUME_PRACTICE
            },
            onClick = if (isRecording) onPauseToggle else onStartRecording,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(GolfThemeUtils.SpacingMedium.dp)
        )
        
        // Bottom: Contextual quick actions
        ContextualQuickActions(
            isRecording = isRecording,
            isPaused = isPaused,
            hasLastSwing = swingAnalysis != null,
            onActionClick = { action ->
                when (action.id) {
                    "pause_practice" -> onPauseToggle()
                    "resume_practice" -> onStartRecording()
                    "switch_mode" -> onShowModeSelector()
                    "review_last_swing" -> { /* Handle review */ }
                    "get_tips" -> { /* Handle tips */ }
                    "save_swing" -> { /* Handle save */ }
                    "camera_settings" -> { /* Handle camera settings */ }
                    "voice_feedback" -> { /* Handle voice feedback */ }
                    "view_stats" -> { /* Handle view stats */ }
                }
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(GolfThemeUtils.SpacingMedium.dp)
        )
    }
}

@Composable
fun SimplifiedStatusIndicator(
    swingPhase: SwingPhase,
    practiceMode: PracticeMode?,
    sessionStats: RealTimeCoachingEngine.SwingSession,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .accessibleCard(
                AccessibilityUtils.getSwingPhaseDescription(swingPhase) + 
                ". Practice mode: ${practiceMode?.name ?: "None"}. " +
                AccessibilityUtils.getSessionStatsDescription(sessionStats)
            ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
        ),
        shape = RoundedCornerShape(GolfThemeUtils.CornerRadiusLarge.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(GolfThemeUtils.SpacingMedium.dp),
            horizontalArrangement = Arrangement.spacedBy(GolfThemeUtils.SpacingMedium.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Swing phase indicator
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = getSwingPhaseColor(swingPhase.name)
                ),
                shape = RoundedCornerShape(GolfThemeUtils.CornerRadiusSmall.dp)
            ) {
                Text(
                    text = swingPhase.name,
                    modifier = Modifier.padding(
                        horizontal = GolfThemeUtils.SpacingSmall.dp,
                        vertical = GolfThemeUtils.SpacingTiny.dp
                    ),
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = GolfTextStyles.SwingPhaseIndicator
                )
            }
            
            // Practice mode indicator
            practiceMode?.let { mode ->
                Text(
                    text = mode.name,
                    style = GolfTextStyles.SessionStat,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            // Essential stats only
            Text(
                text = "${sessionStats.swingCount} swings",
                style = GolfTextStyles.SessionStat,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun EssentialFeedbackCard(
    feedback: RealTimeCoachingEngine.CoachingFeedback?,
    swingAnalysis: RealTimeCoachingEngine.SwingAnalysis?,
    modifier: Modifier = Modifier
) {
    // Only show if there's important feedback
    if (feedback?.severity == RealTimeCoachingEngine.FeedbackSeverity.CRITICAL ||
        feedback?.severity == RealTimeCoachingEngine.FeedbackSeverity.WARNING ||
        swingAnalysis != null) {
        
        Card(
            modifier = modifier
                .accessibleCard(
                    AccessibilityUtils.getFeedbackSeverityDescription(feedback?.severity) + 
                    ". ${feedback?.message ?: ""}"
                ),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
            ),
            shape = RoundedCornerShape(GolfThemeUtils.CornerRadiusMedium.dp)
        ) {
            Column(
                modifier = Modifier.padding(GolfThemeUtils.SpacingMedium.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Show swing analysis score prominently if available
                swingAnalysis?.let { analysis ->
                    Text(
                        text = String.format("%.1f", analysis.overallScore),
                        style = GolfTextStyles.ScoreDisplay,
                        color = getScoreColor(analysis.overallScore),
                        fontWeight = FontWeight.Bold
                    )
                    
                    Text(
                        text = "out of 10",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    
                    if (analysis.areasForImprovement.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(GolfThemeUtils.SpacingSmall.dp))
                        Text(
                            text = analysis.areasForImprovement.first(),
                            style = GolfTextStyles.FeedbackText,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center
                        )
                    }
                } ?: run {
                    // Show critical feedback only
                    feedback?.let { fb ->
                        Text(
                            text = fb.message,
                            style = GolfTextStyles.FeedbackText,
                            color = getFeedbackSeverityColor(fb.severity?.name),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

// Legacy components removed to reduce UI clutter
// Essential feedback is now integrated into EssentialFeedbackCard

// Legacy control buttons replaced with quick actions and FAB
// This provides a cleaner, more accessible interface

@Composable
fun PermissionRequestScreen(
    onRequestPermission: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(GolfThemeUtils.SpacingExtraLarge.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Camera Permission Required",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(GolfThemeUtils.SpacingMedium.dp))
        
        Text(
            text = "This app needs camera access to analyze your golf swing in real-time and provide personalized coaching feedback.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(GolfThemeUtils.SpacingLarge.dp))
        
        Button(
            onClick = onRequestPermission,
            modifier = Modifier
                .fillMaxWidth()
                .accessibleButton(
                    AccessibilityUtils.ContentDescriptions.CAMERA_PREVIEW
                )
        ) {
            Text("Grant Camera Permission")
        }
    }
}

// Helper functions now use theme colors for consistency
// Color utilities are centralized in theme package for better maintainability