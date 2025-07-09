package com.swingsync.ai.ui.screens.coaching

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.swingsync.ai.ui.theme.GolfGreen
import com.swingsync.ai.ui.theme.GolfGold
import com.swingsync.ai.ui.theme.GolfError
import com.swingsync.ai.voice.VoiceState
import kotlinx.coroutines.launch

@Composable
fun CoachingScreen(
    navController: NavHostController,
    viewModel: CoachingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val voiceState by viewModel.voiceState.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val currentPersonality by viewModel.currentPersonality.collectAsState()
    val isSpeaking by viewModel.isSpeaking.collectAsState()
    
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    
    // Auto-scroll to latest message
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            scope.launch {
                listState.animateScrollToItem(messages.size - 1)
            }
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top App Bar with Coach Info
        CoachingTopBar(
            personality = currentPersonality,
            isSpeaking = isSpeaking,
            onMuteToggle = viewModel::toggleMute,
            isMuted = uiState.isMuted
        )
        
        // Messages List
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            items(messages) { message ->
                MessageBubble(
                    message = message,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            // Typing indicator
            if (uiState.isCoachTyping) {
                item {
                    TypingIndicator(
                        modifier = Modifier.padding(start = 16.dp)
                    )
                }
            }
        }
        
        // Voice Control Section
        VoiceControlSection(
            voiceState = voiceState,
            onStartListening = viewModel::startListening,
            onStopListening = viewModel::stopListening,
            onSendMessage = viewModel::sendTextMessage,
            currentInput = uiState.currentInput,
            onInputChange = viewModel::updateInput,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        )
        
        // Quick Actions
        QuickActionsRow(
            onQuickAction = viewModel::sendQuickMessage,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

@Composable
fun CoachingTopBar(
    personality: CoachingPersonality?,
    isSpeaking: Boolean,
    onMuteToggle: () -> Unit,
    isMuted: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Coach Avatar
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(GolfGreen),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = personality?.name?.first()?.uppercaseChar()?.toString() ?: "C",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = personality?.displayName ?: "AI Coach",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = when {
                        isSpeaking -> "Speaking..."
                        else -> personality?.style?.replaceFirstChar { it.uppercaseChar() } ?: "Ready to help"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isSpeaking) GolfGreen else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            
            // Mute/Unmute button
            IconButton(
                onClick = onMuteToggle
            ) {
                Icon(
                    imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                    contentDescription = if (isMuted) "Unmute" else "Mute",
                    tint = if (isMuted) GolfError else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun MessageBubble(
    message: ChatMessage,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = if (message.isFromUser) Arrangement.End else Arrangement.Start
    ) {
        Card(
            modifier = Modifier.widthIn(max = 280.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (message.isFromUser) {
                    GolfGreen
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
            ),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (message.isFromUser) 16.dp else 4.dp,
                bottomEnd = if (message.isFromUser) 4.dp else 16.dp
            )
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (message.isFromUser) {
                        Color.White
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                
                if (message.timestamp != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = message.timestamp,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (message.isFromUser) {
                            Color.White.copy(alpha = 0.7f)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        },
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
fun TypingIndicator(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(3) { index ->
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            CircleShape
                        )
                )
                if (index < 2) {
                    Spacer(modifier = Modifier.width(4.dp))
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Coach is typing...",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun VoiceControlSection(
    voiceState: VoiceState,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    onSendMessage: (String) -> Unit,
    currentInput: String,
    onInputChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Voice status
            Text(
                text = when (voiceState) {
                    VoiceState.IDLE -> "Tap to speak or type your message"
                    VoiceState.LISTENING -> "Listening... Speak now"
                    VoiceState.SPEAKING -> "Coach is speaking..."
                    VoiceState.PROCESSING -> "Processing your message..."
                    VoiceState.ERROR -> "Voice error - try again"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = when (voiceState) {
                    VoiceState.LISTENING -> GolfGreen
                    VoiceState.ERROR -> GolfError
                    else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                },
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Voice button
                FloatingActionButton(
                    onClick = {
                        when (voiceState) {
                            VoiceState.IDLE -> onStartListening()
                            VoiceState.LISTENING -> onStopListening()
                            else -> { /* Do nothing while processing */ }
                        }
                    },
                    modifier = Modifier.size(48.dp),
                    containerColor = when (voiceState) {
                        VoiceState.LISTENING -> GolfError
                        else -> GolfGreen
                    }
                ) {
                    Icon(
                        imageVector = when (voiceState) {
                            VoiceState.LISTENING -> Icons.Default.Stop
                            else -> Icons.Default.Mic
                        },
                        contentDescription = "Voice Control",
                        tint = Color.White
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                // Text input
                OutlinedTextField(
                    value = currentInput,
                    onValueChange = onInputChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Type a message...") },
                    singleLine = true,
                    trailingIcon = {
                        if (currentInput.isNotBlank()) {
                            IconButton(
                                onClick = { onSendMessage(currentInput) }
                            ) {
                                Icon(
                                    Icons.Default.Send,
                                    contentDescription = "Send",
                                    tint = GolfGreen
                                )
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun QuickActionsRow(
    onQuickAction: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val quickActions = listOf(
        "Analyze my swing",
        "Give me tips",
        "What should I work on?",
        "Good job!"
    )
    
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(quickActions) { action ->
            AssistChip(
                onClick = { onQuickAction(action) },
                label = { Text(action) },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }
    }
}

// Data classes
data class ChatMessage(
    val content: String,
    val isFromUser: Boolean,
    val timestamp: String? = null
)

data class CoachingPersonality(
    val name: String,
    val displayName: String,
    val style: String,
    val voiceSettings: VoiceSettings = VoiceSettings()
)

data class VoiceSettings(
    val speed: Float = 1.0f,
    val pitch: Float = 1.0f,
    val volume: Float = 1.0f
)