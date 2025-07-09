package com.golfswing.vro.pixel.ui.camera

import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.golfswing.vro.pixel.coaching.CelebrationSystem
import com.golfswing.vro.pixel.coaching.ConversationalCoachingEngine
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.isGranted
import kotlinx.coroutines.delay

/**
 * Simplified camera screen focused on conversational coaching
 * Clean, minimal UI that doesn't distract from practice
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun SimpleCoachingCameraScreen(
    viewModel: ConversationalCameraViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    val cameraPermissionState = rememberPermissionState(
        android.Manifest.permission.CAMERA
    )
    
    // State
    val coachMessage by viewModel.coachMessage.collectAsState()
    val sessionMood by viewModel.sessionMood.collectAsState()
    val celebration by viewModel.celebration.collectAsState()
    val isRecording by viewModel.isRecording.collectAsState()
    val swingCount by viewModel.swingCount.collectAsState()
    
    if (cameraPermissionState.status.isGranted) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // Camera Preview (Full Screen)
            AndroidView(
                factory = { context ->
                    PreviewView(context).apply {
                        viewModel.initializeCamera(lifecycleOwner, this)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
            
            // Minimal overlay
            Box(modifier = Modifier.fillMaxSize()) {
                // Top: Simple status
                SimpleStatusBar(
                    swingCount = swingCount,
                    mood = sessionMood,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 48.dp)
                )
                
                // Center: Coach message (only when present)
                coachMessage?.let { message ->
                    CoachMessageBubble(
                        message = message,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(horizontal = 32.dp)
                    )
                }
                
                // Celebration overlay
                celebration?.let { event ->
                    CelebrationOverlay(
                        celebration = event,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                
                // Bottom: Single action button
                SimpleActionButton(
                    isRecording = isRecording,
                    onClick = { 
                        if (isRecording) {
                            viewModel.pauseSession()
                        } else {
                            viewModel.startSession()
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 48.dp)
                )
            }
        }
    } else {
        // Simple permission screen
        SimplePermissionScreen(
            onRequestPermission = { cameraPermissionState.launchPermissionRequest() }
        )
    }
}

/**
 * Minimal status bar showing only essential info
 */
@Composable
fun SimpleStatusBar(
    swingCount: Int,
    mood: ConversationalCoachingEngine.SessionMood,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.7f)
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Mood emoji
            Text(
                text = getMoodEmoji(mood),
                fontSize = 20.sp
            )
            
            // Swing count
            Text(
                text = "$swingCount swings",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * Coach message bubble with personality
 */
@Composable
fun CoachMessageBubble(
    message: ConversationalCoachingEngine.CoachMessage,
    modifier: Modifier = Modifier
) {
    var isVisible by remember { mutableStateOf(true) }
    
    LaunchedEffect(message) {
        isVisible = true
        delay(message.duration)
        isVisible = false
    }
    
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut(),
        modifier = modifier
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = getEmotionColor(message.emotion).copy(alpha = 0.95f)
            ),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Emotion icon
                Text(
                    text = getEmotionEmoji(message.emotion),
                    fontSize = 24.sp
                )
                
                // Message
                Text(
                    text = message.message,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * Celebration overlay with animations
 */
@Composable
fun CelebrationOverlay(
    celebration: CelebrationSystem.CelebrationEvent,
    modifier: Modifier = Modifier
) {
    var isVisible by remember { mutableStateOf(true) }
    
    LaunchedEffect(celebration) {
        isVisible = true
        delay(3000)
        isVisible = false
    }
    
    if (isVisible) {
        Box(modifier = modifier) {
            // Particle effects would go here
            CelebrationParticles(celebration)
            
            // Celebration text
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn() + slideInVertically() + scaleIn(),
                exit = fadeOut() + slideOutVertically() + scaleOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 120.dp)
            ) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFFD700).copy(alpha = 0.9f)
                    ),
                    shape = RoundedCornerShape(30.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 32.dp, vertical = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = getCelebrationTitle(celebration),
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1B5E20)
                        )
                        
                        getCelebrationSubtitle(celebration)?.let { subtitle ->
                            Text(
                                text = subtitle,
                                fontSize = 18.sp,
                                color = Color(0xFF2E7D32),
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Simple particle animation for celebrations
 */
@Composable
fun CelebrationParticles(celebration: CelebrationSystem.CelebrationEvent) {
    val particles = remember { List(20) { Particle() } }
    
    particles.forEach { particle ->
        val infiniteTransition = rememberInfiniteTransition()
        
        val offsetY by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = -1000f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = particle.duration,
                    easing = LinearEasing
                ),
                repeatMode = RepeatMode.Restart
            )
        )
        
        val alpha by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 0f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = particle.duration,
                    easing = LinearEasing
                ),
                repeatMode = RepeatMode.Restart
            )
        )
        
        Box(
            modifier = Modifier
                .offset(x = particle.x.dp, y = (particle.startY + offsetY).dp)
                .size(particle.size.dp)
                .alpha(alpha)
                .background(
                    color = particle.color,
                    shape = if (particle.isCircle) CircleShape else RoundedCornerShape(2.dp)
                )
        )
    }
}

/**
 * Single action button - big and easy to tap
 */
@Composable
fun SimpleActionButton(
    isRecording: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val buttonScale by animateFloatAsState(
        targetValue = if (isRecording) 1.1f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )
    
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier
            .size(80.dp)
            .scale(buttonScale),
        containerColor = if (isRecording) Color(0xFFFF5252) else Color(0xFF4CAF50),
        contentColor = Color.White,
        shape = CircleShape
    ) {
        Icon(
            imageVector = if (isRecording) Icons.Default.Pause else Icons.Default.PlayArrow,
            contentDescription = if (isRecording) "Pause" else "Start",
            modifier = Modifier.size(40.dp)
        )
    }
}

/**
 * Simple permission request screen
 */
@Composable
fun SimplePermissionScreen(
    onRequestPermission: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1B5E20),
                        Color(0xFF2E7D32)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = Color(0xFF2E7D32)
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "Let's Practice! 🏌️",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1B5E20)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "I need to see your swing to help you improve. Ready to start?",
                    fontSize = 16.sp,
                    color = Color(0xFF424242),
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Button(
                    onClick = onRequestPermission,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50)
                    ),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Text(
                        text = "Let's Go!",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// Helper functions
private fun getMoodEmoji(mood: ConversationalCoachingEngine.SessionMood): String {
    return when (mood) {
        ConversationalCoachingEngine.SessionMood.WARMING_UP -> "🌅"
        ConversationalCoachingEngine.SessionMood.IN_THE_ZONE -> "🎯"
        ConversationalCoachingEngine.SessionMood.STRUGGLING -> "💪"
        ConversationalCoachingEngine.SessionMood.IMPROVING -> "📈"
        ConversationalCoachingEngine.SessionMood.CRUSHING_IT -> "🔥"
        ConversationalCoachingEngine.SessionMood.NEUTRAL -> "⛳"
    }
}

private fun getEmotionColor(emotion: ConversationalCoachingEngine.CoachEmotion): Color {
    return when (emotion) {
        ConversationalCoachingEngine.CoachEmotion.ENCOURAGING -> Color(0xFF4CAF50)
        ConversationalCoachingEngine.CoachEmotion.EXCITED -> Color(0xFFFFC107)
        ConversationalCoachingEngine.CoachEmotion.HELPFUL -> Color(0xFF2196F3)
        ConversationalCoachingEngine.CoachEmotion.SUPPORTIVE -> Color(0xFF9C27B0)
        ConversationalCoachingEngine.CoachEmotion.PROUD -> Color(0xFFFF9800)
        ConversationalCoachingEngine.CoachEmotion.THOUGHTFUL -> Color(0xFF607D8B)
        ConversationalCoachingEngine.CoachEmotion.CELEBRATING -> Color(0xFFFFD700)
    }
}

private fun getEmotionEmoji(emotion: ConversationalCoachingEngine.CoachEmotion): String {
    return when (emotion) {
        ConversationalCoachingEngine.CoachEmotion.ENCOURAGING -> "😊"
        ConversationalCoachingEngine.CoachEmotion.EXCITED -> "🤩"
        ConversationalCoachingEngine.CoachEmotion.HELPFUL -> "🤔"
        ConversationalCoachingEngine.CoachEmotion.SUPPORTIVE -> "🤗"
        ConversationalCoachingEngine.CoachEmotion.PROUD -> "😍"
        ConversationalCoachingEngine.CoachEmotion.THOUGHTFUL -> "🧐"
        ConversationalCoachingEngine.CoachEmotion.CELEBRATING -> "🎉"
    }
}

private fun getCelebrationTitle(celebration: CelebrationSystem.CelebrationEvent): String {
    return when (celebration) {
        CelebrationSystem.CelebrationEvent.GREAT_SWING -> "Great Swing! 🎯"
        CelebrationSystem.CelebrationEvent.PERFECT_SWING -> "PERFECT! 🏆"
        CelebrationSystem.CelebrationEvent.CONSISTENCY_STREAK -> "On Fire! 🔥"
        CelebrationSystem.CelebrationEvent.PERSONAL_BEST -> "NEW RECORD! 🌟"
        CelebrationSystem.CelebrationEvent.MILESTONE_REACHED -> "Milestone! 🎉"
        CelebrationSystem.CelebrationEvent.IMPROVEMENT_DETECTED -> "Improving! 📈"
        CelebrationSystem.CelebrationEvent.FIRST_SWING -> "First Swing! 🎊"
        CelebrationSystem.CelebrationEvent.COMEBACK -> "Great Comeback! 💪"
        CelebrationSystem.CelebrationEvent.PRACTICE_MILESTONE -> "Dedication! ⏱️"
        CelebrationSystem.CelebrationEvent.TECHNIQUE_MASTERY -> "Mastered! 🎓"
    }
}

private fun getCelebrationSubtitle(celebration: CelebrationSystem.CelebrationEvent): String? {
    return when (celebration) {
        CelebrationSystem.CelebrationEvent.CONSISTENCY_STREAK -> "Keep it going!"
        CelebrationSystem.CelebrationEvent.PERSONAL_BEST -> "You're getting better!"
        CelebrationSystem.CelebrationEvent.MILESTONE_REACHED -> "Hard work pays off!"
        else -> null
    }
}

// Simple particle data class
private data class Particle(
    val x: Float = (-200..200).random().toFloat(),
    val startY: Float = (600..800).random().toFloat(),
    val size: Float = (8..16).random().toFloat(),
    val duration: Int = (2000..4000).random(),
    val color: Color = listOf(
        Color(0xFFFFD700),
        Color(0xFFFFA500),
        Color(0xFFFF6347),
        Color(0xFF4CAF50),
        Color(0xFF2196F3)
    ).random(),
    val isCircle: Boolean = listOf(true, false).random()
)