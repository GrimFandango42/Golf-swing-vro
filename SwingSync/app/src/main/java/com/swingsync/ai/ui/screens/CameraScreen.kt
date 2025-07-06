package com.swingsync.ai.ui.screens

import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.swingsync.ai.ui.viewmodels.CameraViewModel
import com.swingsync.ai.utils.camera.RecordingState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAnalysis: (String) -> Unit,
    viewModel: CameraViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsState()
    
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    
    LaunchedEffect(previewView) {
        previewView?.let { preview ->
            viewModel.startCamera(lifecycleOwner, preview)
        }
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        // Camera Preview
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    previewView = this
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        
        // Top Bar
        TopAppBar(
            title = { Text("Record Swing") },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
            ),
            modifier = Modifier.align(Alignment.TopCenter)
        )
        
        // Pose Estimation Overlay
        if (uiState.showPoseOverlay) {
            PoseOverlayView(
                poseResults = uiState.poseResults,
                modifier = Modifier.fillMaxSize()
            )
        }
        
        // Recording Controls
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Swing Phase Indicator
            if (uiState.currentPhase.isNotEmpty()) {
                Card(
                    modifier = Modifier.padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                    )
                ) {
                    Text(
                        text = uiState.currentPhase,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
            
            // Recording Timer
            if (uiState.isRecording) {
                Text(
                    text = uiState.recordingTime,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
            
            // Control Buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Settings Button
                FloatingActionButton(
                    onClick = { viewModel.toggleSettings() },
                    modifier = Modifier.size(48.dp),
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings")
                }
                
                // Record Button
                FloatingActionButton(
                    onClick = { 
                        if (uiState.isRecording) {
                            viewModel.stopRecording()
                        } else {
                            viewModel.startRecording()
                        }
                    },
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape),
                    containerColor = if (uiState.isRecording) 
                        MaterialTheme.colorScheme.error 
                    else 
                        MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        if (uiState.isRecording) Icons.Default.Stop else Icons.Default.FiberManualRecord,
                        contentDescription = if (uiState.isRecording) "Stop" else "Record",
                        modifier = Modifier.size(32.dp)
                    )
                }
                
                // Voice Toggle Button
                FloatingActionButton(
                    onClick = { viewModel.toggleVoiceCoaching() },
                    modifier = Modifier.size(48.dp),
                    containerColor = if (uiState.isVoiceCoachingEnabled) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.surface
                ) {
                    Icon(
                        if (uiState.isVoiceCoachingEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                        contentDescription = "Voice Coaching"
                    )
                }
            }
        }
        
        // Settings Panel
        if (uiState.showSettings) {
            CameraSettingsPanel(
                settings = uiState.cameraSettings,
                onSettingChanged = { setting, value -> viewModel.updateSetting(setting, value) },
                onDismiss = { viewModel.toggleSettings() },
                modifier = Modifier.align(Alignment.CenterEnd)
            )
        }
        
        // Loading Indicator
        if (uiState.isProcessing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Card {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Processing swing analysis...")
                    }
                }
            }
        }
    }
    
    // Handle recording completion
    LaunchedEffect(uiState.recordingState) {
        if (uiState.recordingState == RecordingState.FINISHED && uiState.analysisId.isNotEmpty()) {
            onNavigateToAnalysis(uiState.analysisId)
        }
    }
}

@Composable
fun PoseOverlayView(
    poseResults: List<Any>, // Replace with actual pose result type
    modifier: Modifier = Modifier
) {
    // Implement pose overlay drawing
    // This would draw the pose landmarks and connections over the camera preview
}

@Composable
fun CameraSettingsPanel(
    settings: Map<String, Any>,
    onSettingChanged: (String, Any) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .padding(16.dp)
            .width(300.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Camera Settings",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Video Quality Setting
            Text("Video Quality", style = MaterialTheme.typography.bodyMedium)
            // Add slider or dropdown for video quality
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Analysis Sensitivity Setting
            Text("Analysis Sensitivity", style = MaterialTheme.typography.bodyMedium)
            // Add slider for analysis sensitivity
        }
    }
}