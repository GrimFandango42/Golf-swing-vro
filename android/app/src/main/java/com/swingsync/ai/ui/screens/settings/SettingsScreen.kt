package com.swingsync.ai.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.swingsync.ai.ui.theme.GolfGreen

@Composable
fun SettingsScreen(
    navController: NavHostController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val personalities by viewModel.availablePersonalities.collectAsState()
    val currentPersonality by viewModel.currentPersonality.collectAsState()
    
    var showPersonalityDialog by remember { mutableStateOf(false) }
    var showVoiceSettingsDialog by remember { mutableStateOf(false) }
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header
        item {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        
        // Coaching Personality Section
        item {
            SettingsSection(title = "Coaching Preferences") {
                SettingsItem(
                    title = "Coaching Personality",
                    subtitle = currentPersonality?.displayName ?: "Select a personality",
                    icon = Icons.Default.Person,
                    onClick = { showPersonalityDialog = true }
                )
            }
        }
        
        // Voice Settings Section
        item {
            SettingsSection(title = "Voice Settings") {
                SettingsItem(
                    title = "Voice & Speech",
                    subtitle = "Adjust voice settings and speed",
                    icon = Icons.Default.VolumeUp,
                    onClick = { showVoiceSettingsDialog = true }
                )
                
                Divider(modifier = Modifier.padding(horizontal = 16.dp))
                
                SettingsToggle(
                    title = "Voice Feedback",
                    subtitle = "Enable spoken coaching feedback",
                    checked = uiState.voiceFeedbackEnabled,
                    onCheckedChange = viewModel::setVoiceFeedbackEnabled
                )
                
                SettingsToggle(
                    title = "Auto-Listen",
                    subtitle = "Automatically listen for voice commands",
                    checked = uiState.autoListenEnabled,
                    onCheckedChange = viewModel::setAutoListenEnabled
                )
            }
        }
        
        // Notifications Section
        item {
            SettingsSection(title = "Notifications") {
                SettingsToggle(
                    title = "Practice Reminders",
                    subtitle = "Daily practice session reminders",
                    checked = uiState.practiceRemindersEnabled,
                    onCheckedChange = viewModel::setPracticeRemindersEnabled
                )
                
                SettingsToggle(
                    title = "Progress Updates",
                    subtitle = "Weekly progress summaries",
                    checked = uiState.progressUpdatesEnabled,
                    onCheckedChange = viewModel::setProgressUpdatesEnabled
                )
            }
        }
        
        // Analysis Settings Section
        item {
            SettingsSection(title = "Analysis Preferences") {
                SettingsSlider(
                    title = "Analysis Sensitivity",
                    subtitle = "How detailed should the swing analysis be",
                    value = uiState.analysisSensitivity,
                    onValueChange = viewModel::setAnalysisSensitivity
                )
                
                SettingsToggle(
                    title = "Real-time Feedback",
                    subtitle = "Show feedback during swing recording",
                    checked = uiState.realtimeFeedbackEnabled,
                    onCheckedChange = viewModel::setRealtimeFeedbackEnabled
                )
            }
        }
        
        // About Section
        item {
            SettingsSection(title = "About") {
                SettingsItem(
                    title = "App Version",
                    subtitle = "1.0.0",
                    icon = Icons.Default.Info,
                    onClick = { }
                )
            }
        }
    }
    
    // Personality Selection Dialog
    if (showPersonalityDialog) {
        PersonalitySelectionDialog(
            personalities = personalities,
            currentPersonality = currentPersonality,
            onPersonalitySelected = { personality ->
                viewModel.setCoachingPersonality(personality)
                showPersonalityDialog = false
            },
            onDismiss = { showPersonalityDialog = false }
        )
    }
    
    // Voice Settings Dialog
    if (showVoiceSettingsDialog) {
        VoiceSettingsDialog(
            voiceSettings = uiState.voiceSettings,
            onSettingsChanged = viewModel::updateVoiceSettings,
            onDismiss = { showVoiceSettingsDialog = false }
        )
    }
}

@Composable
fun SettingsSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
        )
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column {
                content()
            }
        }
    }
}

@Composable
fun SettingsItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun SettingsToggle(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
        
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
fun SettingsSlider(
    title: String,
    subtitle: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
        
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..1f,
            steps = 10
        )
    }
}

@Composable
fun PersonalitySelectionDialog(
    personalities: List<CoachingPersonality>,
    currentPersonality: CoachingPersonality?,
    onPersonalitySelected: (CoachingPersonality) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Choose Your Coach",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            LazyColumn {
                items(personalities) { personality ->
                    PersonalityOption(
                        personality = personality,
                        isSelected = personality.name == currentPersonality?.name,
                        onSelected = { onPersonalitySelected(personality) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}

@Composable
fun PersonalityOption(
    personality: CoachingPersonality,
    isSelected: Boolean,
    onSelected: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = isSelected,
                onClick = onSelected
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onSelected
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = personality.displayName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            
            Text(
                text = personality.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
        
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Selected",
                tint = GolfGreen
            )
        }
    }
}

@Composable
fun VoiceSettingsDialog(
    voiceSettings: VoiceSettingsData,
    onSettingsChanged: (VoiceSettingsData) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Voice Settings",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = "Speech Speed",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                
                Slider(
                    value = voiceSettings.speed,
                    onValueChange = { speed ->
                        onSettingsChanged(voiceSettings.copy(speed = speed))
                    },
                    valueRange = 0.5f..2.0f,
                    steps = 15
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Voice Pitch",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                
                Slider(
                    value = voiceSettings.pitch,
                    onValueChange = { pitch ->
                        onSettingsChanged(voiceSettings.copy(pitch = pitch))
                    },
                    valueRange = 0.5f..2.0f,
                    steps = 15
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Volume",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                
                Slider(
                    value = voiceSettings.volume,
                    onValueChange = { volume ->
                        onSettingsChanged(voiceSettings.copy(volume = volume))
                    },
                    valueRange = 0.1f..1.0f,
                    steps = 9
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}

data class CoachingPersonality(
    val name: String,
    val displayName: String,
    val description: String,
    val style: String
)

data class VoiceSettingsData(
    val speed: Float = 1.0f,
    val pitch: Float = 1.0f,
    val volume: Float = 1.0f
)