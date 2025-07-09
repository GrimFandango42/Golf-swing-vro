package com.golfswing.vro.pixel.ui.practice

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GolfCourse
import androidx.compose.material.icons.filled.Sports
import androidx.compose.material.icons.filled.SportsGolf
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.golfswing.vro.pixel.ui.accessibility.AccessibilityUtils
import com.golfswing.vro.pixel.ui.accessibility.accessibleButton
import com.golfswing.vro.pixel.ui.accessibility.accessibleCard
import com.golfswing.vro.pixel.ui.theme.GolfTextStyles
import com.golfswing.vro.pixel.ui.theme.GolfThemeUtils

// Practice mode data class
data class PracticeMode(
    val id: String,
    val name: String,
    val description: String,
    val icon: ImageVector,
    val difficulty: PracticeDifficulty,
    val focusAreas: List<String>,
    val estimatedDuration: String,
    val tips: List<String> = emptyList()
)

enum class PracticeDifficulty {
    BEGINNER,
    INTERMEDIATE,
    ADVANCED
}

// Available practice modes
object PracticeModes {
    val FULL_SWING = PracticeMode(
        id = "full_swing",
        name = "Full Swing",
        description = "Practice your complete golf swing motion",
        icon = Icons.Default.SportsGolf,
        difficulty = PracticeDifficulty.INTERMEDIATE,
        focusAreas = listOf("Posture", "Swing Path", "Impact", "Follow-through"),
        estimatedDuration = "15-20 min",
        tips = listOf(
            "Focus on smooth tempo",
            "Keep your head steady",
            "Complete your follow-through"
        )
    )
    
    val PUTTING = PracticeMode(
        id = "putting",
        name = "Putting",
        description = "Improve your putting accuracy and consistency",
        icon = Icons.Default.GolfCourse,
        difficulty = PracticeDifficulty.BEGINNER,
        focusAreas = listOf("Alignment", "Stroke Path", "Distance Control"),
        estimatedDuration = "10-15 min",
        tips = listOf(
            "Keep your head still",
            "Smooth, pendulum motion",
            "Focus on the target line"
        )
    )
    
    val CHIPPING = PracticeMode(
        id = "chipping",
        name = "Chipping",
        description = "Work on your short game around the green",
        icon = Icons.Default.Sports,
        difficulty = PracticeDifficulty.BEGINNER,
        focusAreas = listOf("Ball Position", "Weight Transfer", "Club Selection"),
        estimatedDuration = "10-15 min",
        tips = listOf(
            "Ball back in stance",
            "Weight on front foot",
            "Accelerate through impact"
        )
    )
    
    val DRIVING = PracticeMode(
        id = "driving",
        name = "Driving",
        description = "Focus on distance and accuracy off the tee",
        icon = Icons.Default.SportsGolf,
        difficulty = PracticeDifficulty.INTERMEDIATE,
        focusAreas = listOf("Setup", "Shoulder Turn", "Hip Rotation", "Timing"),
        estimatedDuration = "20-25 min",
        tips = listOf(
            "Tee ball at proper height",
            "Wide stance for stability",
            "Turn shoulders fully"
        )
    )
    
    val IRON_PLAY = PracticeMode(
        id = "iron_play",
        name = "Iron Play",
        description = "Master your mid-iron consistency",
        icon = Icons.Default.Sports,
        difficulty = PracticeDifficulty.INTERMEDIATE,
        focusAreas = listOf("Ball Striking", "Divot Control", "Trajectory"),
        estimatedDuration = "15-20 min",
        tips = listOf(
            "Ball-first contact",
            "Compress the ball",
            "Maintain spine angle"
        )
    )
    
    val WEDGE_PLAY = PracticeMode(
        id = "wedge_play",
        name = "Wedge Play",
        description = "Develop scoring shot precision",
        icon = Icons.Default.Sports,
        difficulty = PracticeDifficulty.ADVANCED,
        focusAreas = listOf("Distance Control", "Spin", "Trajectory Control"),
        estimatedDuration = "15-20 min",
        tips = listOf(
            "Control swing length",
            "Maintain rhythm",
            "Focus on clean contact"
        )
    )
    
    val ALL_MODES = listOf(
        FULL_SWING,
        PUTTING,
        CHIPPING,
        DRIVING,
        IRON_PLAY,
        WEDGE_PLAY
    )
}

@Composable
fun PracticeModeSelector(
    selectedMode: PracticeMode?,
    onModeSelected: (PracticeMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(GolfThemeUtils.SpacingMedium.dp)
    ) {
        Text(
            text = "Choose Practice Mode",
            style = GolfTextStyles.QuickActionLabel,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = GolfThemeUtils.SpacingSmall.dp)
        )
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(GolfThemeUtils.SpacingSmall.dp),
            contentPadding = PaddingValues(horizontal = GolfThemeUtils.SpacingSmall.dp)
        ) {
            items(PracticeModes.ALL_MODES) { mode ->
                PracticeModeCard(
                    mode = mode,
                    isSelected = selectedMode?.id == mode.id,
                    onClick = { onModeSelected(mode) }
                )
            }
        }
        
        // Show details for selected mode
        selectedMode?.let { mode ->
            Spacer(modifier = Modifier.height(GolfThemeUtils.SpacingMedium.dp))
            PracticeModeDetails(mode = mode)
        }
    }
}

@Composable
fun PracticeModeCard(
    mode: PracticeMode,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .width(160.dp)
            .accessibleCard(
                contentDescription = "${mode.name} practice mode. ${mode.description}. " +
                        "Difficulty: ${mode.difficulty.name.lowercase()}. " +
                        "Duration: ${mode.estimatedDuration}"
            ),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) {
                GolfThemeUtils.ElevationLarge.dp
            } else {
                GolfThemeUtils.ElevationSmall.dp
            }
        ),
        shape = RoundedCornerShape(GolfThemeUtils.CornerRadiusMedium.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(GolfThemeUtils.SpacingMedium.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Icon
            Icon(
                imageVector = mode.icon,
                contentDescription = null,
                tint = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                modifier = Modifier.size(32.dp)
            )
            
            Spacer(modifier = Modifier.height(GolfThemeUtils.SpacingSmall.dp))
            
            // Mode name
            Text(
                text = mode.name,
                style = GolfTextStyles.QuickActionLabel,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(GolfThemeUtils.SpacingTiny.dp))
            
            // Difficulty indicator
            DifficultyIndicator(
                difficulty = mode.difficulty,
                isSelected = isSelected
            )
            
            Spacer(modifier = Modifier.height(GolfThemeUtils.SpacingTiny.dp))
            
            // Duration
            Text(
                text = mode.estimatedDuration,
                style = MaterialTheme.typography.bodySmall,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                },
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun DifficultyIndicator(
    difficulty: PracticeDifficulty,
    isSelected: Boolean,
    modifier: Modifier = Modifier
) {
    val color = if (isSelected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        repeat(3) { index ->
            val isActive = when (difficulty) {
                PracticeDifficulty.BEGINNER -> index == 0
                PracticeDifficulty.INTERMEDIATE -> index <= 1
                PracticeDifficulty.ADVANCED -> true
            }
            
            Box(
                modifier = Modifier
                    .size(width = 8.dp, height = 4.dp)
                    .background(
                        color = if (isActive) {
                            color
                        } else {
                            color.copy(alpha = 0.3f)
                        },
                        shape = RoundedCornerShape(2.dp)
                    )
            )
        }
    }
}

@Composable
fun PracticeModeDetails(
    mode: PracticeMode,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .accessibleCard(
                contentDescription = "Practice mode details for ${mode.name}"
            ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        shape = RoundedCornerShape(GolfThemeUtils.CornerRadiusMedium.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(GolfThemeUtils.SpacingMedium.dp)
        ) {
            // Description
            Text(
                text = mode.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(GolfThemeUtils.SpacingMedium.dp))
            
            // Focus areas
            Text(
                text = "Focus Areas:",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(GolfThemeUtils.SpacingSmall.dp))
            
            mode.focusAreas.forEach { area ->
                Row(
                    modifier = Modifier.padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "• ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = area,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            
            // Tips (if available)
            if (mode.tips.isNotEmpty()) {
                Spacer(modifier = Modifier.height(GolfThemeUtils.SpacingMedium.dp))
                
                Text(
                    text = "Tips:",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(GolfThemeUtils.SpacingSmall.dp))
                
                mode.tips.forEach { tip ->
                    Row(
                        modifier = Modifier.padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "💡 ",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = tip,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}