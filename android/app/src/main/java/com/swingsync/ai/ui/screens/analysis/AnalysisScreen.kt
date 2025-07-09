package com.swingsync.ai.ui.screens.analysis

import android.Manifest
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.swingsync.ai.ui.theme.GolfGreen
import com.swingsync.ai.ui.theme.GolfGold
import com.swingsync.ai.ui.theme.GolfError
import com.swingsync.ai.voice.VoiceState
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun AnalysisScreen(
    navController: NavHostController,
    viewModel: AnalysisViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    // Camera permission
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    val audioPermissionState = rememberPermissionState(Manifest.permission.RECORD_AUDIO)
    
    // UI State
    val uiState by viewModel.uiState.collectAsState()
    val voiceState by viewModel.voiceState.collectAsState()
    val isRecording by viewModel.isRecording.collectAsState()
    val poseData by viewModel.poseData.collectAsState()
    val feedback by viewModel.realtimeFeedback.collectAsState()
    
    // Camera setup
    LaunchedEffect(cameraPermissionState.status.isGranted) {
        if (cameraPermissionState.status.isGranted) {
            viewModel.initializeCamera()
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                Text(
                    text = "Swing Analysis",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )
        
        // Camera Preview with Pose Overlay
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(3f / 4f)
                .padding(16.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(2.dp, GolfGreen, RoundedCornerShape(12.dp))
        ) {
            if (cameraPermissionState.status.isGranted) {
                CameraPreview(
                    modifier = Modifier.fillMaxSize(),
                    onCameraProvider = viewModel::setCameraProvider
                )
                
                // Pose Overlay
                if (poseData != null) {
                    PoseOverlay(
                        poseData = poseData!!,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                
                // Recording indicator
                if (isRecording) {
                    RecordingIndicator(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                    )
                }
            } else {
                // Camera permission not granted
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Camera access needed to analyze your swing",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { cameraPermissionState.launchPermissionRequest() }
                    ) {
                        Text("Grant Camera Permission")
                    }
                }
            }
        }
        
        // Real-time Feedback Panel
        if (feedback.isNotEmpty()) {
            FeedbackPanel(
                feedback = feedback,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Voice Control Panel
        VoiceControlPanel(
            voiceState = voiceState,
            isRecording = isRecording,
            onStartRecording = viewModel::startRecording,
            onStopRecording = viewModel::stopRecording,
            onStartListening = viewModel::startListening,
            onStopListening = viewModel::stopListening,
            audioPermissionGranted = audioPermissionState.status.isGranted,
            onRequestAudioPermission = { audioPermissionState.launchPermissionRequest() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        )
        
        // Control Buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Analyze Button
            Button(
                onClick = viewModel::analyzeSwing,
                enabled = !isRecording,
                modifier = Modifier.weight(1f)
            ) {
                Text("Analyze Swing")
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Settings Button
            OutlinedButton(
                onClick = { navController.navigate("settings") },
                modifier = Modifier.weight(1f)
            ) {
                Text("Settings")
            }
        }
    }
}

@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    onCameraProvider: (ProcessCameraProvider) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                onCameraProvider(cameraProvider)
                
                val preview = Preview.Builder().build()
                preview.setSurfaceProvider(previewView.surfaceProvider)
                
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                
                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview
                    )
                } catch (exc: Exception) {
                    // Handle camera binding error
                }
            }, ContextCompat.getMainExecutor(ctx))
            
            previewView
        },
        modifier = modifier
    )
}

@Composable
fun PoseOverlay(
    poseData: PoseData,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        drawPose(poseData)
    }
}

private fun DrawScope.drawPose(poseData: PoseData) {
    // Draw pose keypoints and connections
    val keypoints = poseData.keypoints
    val connections = poseData.connections
    
    // Draw connections
    connections.forEach { (start, end) ->
        if (start < keypoints.size && end < keypoints.size) {
            val startPoint = keypoints[start]
            val endPoint = keypoints[end]
            
            if (startPoint.confidence > 0.5f && endPoint.confidence > 0.5f) {
                drawLine(
                    color = GolfGreen,
                    start = androidx.compose.ui.geometry.Offset(
                        startPoint.x * size.width,
                        startPoint.y * size.height
                    ),
                    end = androidx.compose.ui.geometry.Offset(
                        endPoint.x * size.width,
                        endPoint.y * size.height
                    ),
                    strokeWidth = 4.dp.toPx()
                )
            }
        }
    }
    
    // Draw keypoints
    keypoints.forEach { keypoint ->
        if (keypoint.confidence > 0.5f) {
            drawCircle(
                color = GolfGold,
                radius = 8.dp.toPx(),
                center = androidx.compose.ui.geometry.Offset(
                    keypoint.x * size.width,
                    keypoint.y * size.height
                )
            )
        }
    }
}

@Composable
fun RecordingIndicator(
    modifier: Modifier = Modifier
) {
    var isVisible by remember { mutableStateOf(true) }
    
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(500)
            isVisible = !isVisible
        }
    }
    
    if (isVisible) {
        Box(
            modifier = modifier
                .size(12.dp)
                .background(GolfError, CircleShape)
        )
    }
}

@Composable
fun FeedbackPanel(
    feedback: List<String>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Live Feedback",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            feedback.forEach { tip ->
                Row(
                    modifier = Modifier.padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(GolfGreen, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = tip,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
fun VoiceControlPanel(
    voiceState: VoiceState,
    isRecording: Boolean,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    audioPermissionGranted: Boolean,
    onRequestAudioPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Voice Coach",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Voice State Indicator
            Text(
                text = when (voiceState) {
                    VoiceState.IDLE -> "Ready to listen"
                    VoiceState.LISTENING -> "Listening..."
                    VoiceState.SPEAKING -> "Speaking..."
                    VoiceState.PROCESSING -> "Processing..."
                    VoiceState.ERROR -> "Error occurred"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = when (voiceState) {
                    VoiceState.LISTENING -> GolfGreen
                    VoiceState.SPEAKING -> GolfGold
                    VoiceState.ERROR -> GolfError
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (audioPermissionGranted) {
                // Voice Control Button
                FloatingActionButton(
                    onClick = {
                        when (voiceState) {
                            VoiceState.IDLE -> onStartListening()
                            VoiceState.LISTENING -> onStopListening()
                            else -> { /* Do nothing while processing */ }
                        }
                    },
                    modifier = Modifier.size(56.dp),
                    containerColor = when (voiceState) {
                        VoiceState.LISTENING -> GolfError
                        else -> GolfGreen
                    }
                ) {
                    Icon(
                        imageVector = when (voiceState) {
                            VoiceState.LISTENING -> Icons.Default.MicOff
                            else -> Icons.Default.Mic
                        },
                        contentDescription = "Voice Control",
                        tint = Color.White
                    )
                }
            } else {
                Button(
                    onClick = onRequestAudioPermission
                ) {
                    Text("Grant Audio Permission")
                }
            }
        }
    }
}

// Data classes for pose detection
data class PoseData(
    val keypoints: List<Keypoint>,
    val connections: List<Pair<Int, Int>>
)

data class Keypoint(
    val x: Float,
    val y: Float,
    val confidence: Float
)