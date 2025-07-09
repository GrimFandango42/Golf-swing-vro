package com.golfswing.vro.pixel.ui.personal

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.golfswing.vro.pixel.coaching.RealTimeCoachingEngine
import com.golfswing.vro.pixel.ui.accessibility.AccessibilityUtils
import com.golfswing.vro.pixel.ui.accessibility.accessibleCard
import com.golfswing.vro.pixel.ui.theme.GolfTextStyles
import com.golfswing.vro.pixel.ui.theme.GolfThemeUtils
import com.golfswing.vro.pixel.ui.theme.getScoreColor
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

// Personal practice session data
data class PersonalSession(
    val id: String,
    val timestamp: LocalDateTime,
    val practiceMode: String,
    val swingCount: Int,
    val averageScore: Float,
    val bestScore: Float,
    val consistencyScore: Float,
    val totalDuration: Long, // in minutes
    val improvementAreas: List<String>,
    val achievements: List<String> = emptyList()
)

// Personal preferences for practice
data class PersonalPreferences(
    val preferredPracticeMode: String = "full_swing",
    val targetSessionDuration: Int = 20, // minutes
    val targetSwingsPerSession: Int = 50,
    val feedbackVerbosity: FeedbackVerbosity = FeedbackVerbosity.BALANCED,
    val autoSaveEnabled: Boolean = true,
    val voiceFeedbackEnabled: Boolean = false,
    val showDetailedMetrics: Boolean = false,
    val reminderEnabled: Boolean = true,
    val reminderFrequency: Int = 24 // hours
)

enum class FeedbackVerbosity {
    MINIMAL,
    BALANCED,
    DETAILED
}

// Personal progress tracking
data class PersonalProgress(
    val totalSessions: Int,
    val totalSwings: Int,
    val averageScore: Float,
    val bestScore: Float,
    val consistencyImprovement: Float,
    val streakDays: Int,
    val favoriteMode: String,
    val recentTrend: ProgressTrend,
    val weeklyGoalProgress: Float // 0.0 to 1.0
)

enum class ProgressTrend {
    IMPROVING,
    STABLE,
    DECLINING
}

@Composable
fun PersonalDashboard(
    progress: PersonalProgress,
    recentSessions: List<PersonalSession>,
    preferences: PersonalPreferences,
    onStartQuickPractice: () -> Unit,
    onViewProgress: () -> Unit,
    onEditPreferences: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .padding(GolfThemeUtils.SpacingMedium.dp),
        verticalArrangement = Arrangement.spacedBy(GolfThemeUtils.SpacingMedium.dp)
    ) {
        // Welcome header
        item {
            PersonalWelcomeHeader(
                progress = progress,
                onStartQuickPractice = onStartQuickPractice
            )
        }
        
        // Progress overview
        item {
            PersonalProgressOverview(
                progress = progress,
                onViewProgress = onViewProgress
            )
        }
        
        // Quick actions
        item {
            PersonalQuickActions(
                preferences = preferences,
                onEditPreferences = onEditPreferences
            )
        }
        
        // Recent sessions
        item {
            Text(
                text = "Recent Practice Sessions",
                style = GolfTextStyles.QuickActionLabel,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = GolfThemeUtils.SpacingSmall.dp)
            )
        }
        
        items(recentSessions.take(5)) { session ->
            PersonalSessionCard(session = session)
        }
    }
}

@Composable
fun PersonalWelcomeHeader(
    progress: PersonalProgress,
    onStartQuickPractice: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .accessibleCard("Personal practice dashboard welcome"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = RoundedCornerShape(GolfThemeUtils.CornerRadiusMedium.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(GolfThemeUtils.SpacingMedium.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Welcome Back!",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Text(
                        text = "${progress.streakDays} day practice streak",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
                
                FilledTonalButton(
                    onClick = onStartQuickPractice,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("Start Practice")
                }
            }
            
            Spacer(modifier = Modifier.height(GolfThemeUtils.SpacingMedium.dp))
            
            // Weekly goal progress
            LinearProgressIndicator(
                progress = progress.weeklyGoalProgress,
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            )
            
            Spacer(modifier = Modifier.height(GolfThemeUtils.SpacingSmall.dp))
            
            Text(
                text = "Weekly goal: ${(progress.weeklyGoalProgress * 100).toInt()}% complete",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun PersonalProgressOverview(
    progress: PersonalProgress,
    onViewProgress: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .accessibleCard("Personal progress overview"),
        onClick = onViewProgress,
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Progress Overview",
                    style = GolfTextStyles.QuickActionLabel,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
                
                Icon(
                    imageVector = Icons.Default.TrendingUp,
                    contentDescription = null,
                    tint = getProgressTrendColor(progress.recentTrend)
                )
            }
            
            Spacer(modifier = Modifier.height(GolfThemeUtils.SpacingMedium.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ProgressMetric(
                    label = "Sessions",
                    value = progress.totalSessions.toString(),
                    modifier = Modifier.weight(1f)
                )
                
                ProgressMetric(
                    label = "Avg Score",
                    value = String.format("%.1f", progress.averageScore),
                    color = getScoreColor(progress.averageScore),
                    modifier = Modifier.weight(1f)
                )
                
                ProgressMetric(
                    label = "Best Score",
                    value = String.format("%.1f", progress.bestScore),
                    color = getScoreColor(progress.bestScore),
                    modifier = Modifier.weight(1f)
                )
                
                ProgressMetric(
                    label = "Consistency",
                    value = "${(progress.consistencyImprovement * 100).toInt()}%",
                    color = if (progress.consistencyImprovement > 0) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun ProgressMetric(
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = GolfTextStyles.ScoreDisplay,
            color = color,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun PersonalQuickActions(
    preferences: PersonalPreferences,
    onEditPreferences: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .accessibleCard("Personal quick actions"),
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
            Text(
                text = "Quick Actions",
                style = GolfTextStyles.QuickActionLabel,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(GolfThemeUtils.SpacingMedium.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                QuickActionButton(
                    icon = Icons.Default.Person,
                    label = "Preferences",
                    onClick = onEditPreferences,
                    modifier = Modifier.weight(1f)
                )
                
                QuickActionButton(
                    icon = Icons.Default.History,
                    label = "History",
                    onClick = { /* Navigate to history */ },
                    modifier = Modifier.weight(1f)
                )
                
                QuickActionButton(
                    icon = Icons.Default.Settings,
                    label = "Settings",
                    onClick = { /* Navigate to settings */ },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun QuickActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        FilledTonalIconButton(
            onClick = onClick,
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            )
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label
            )
        }
        
        Spacer(modifier = Modifier.height(GolfThemeUtils.SpacingTiny.dp))
        
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun PersonalSessionCard(
    session: PersonalSession,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .accessibleCard(
                "Practice session from ${session.timestamp.format(DateTimeFormatter.ofPattern("MMM dd"))}. " +
                        "${session.swingCount} swings, average score ${String.format("%.1f", session.averageScore)}"
            ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(GolfThemeUtils.CornerRadiusSmall.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(GolfThemeUtils.SpacingMedium.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = session.practiceMode.replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium
                    )
                    
                    Spacer(modifier = Modifier.width(GolfThemeUtils.SpacingSmall.dp))
                    
                    Text(
                        text = session.timestamp.format(DateTimeFormatter.ofPattern("MMM dd")),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
                
                Spacer(modifier = Modifier.height(GolfThemeUtils.SpacingTiny.dp))
                
                Text(
                    text = "${session.swingCount} swings • ${session.totalDuration}min",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = String.format("%.1f", session.averageScore),
                    style = GolfTextStyles.ScoreDisplay,
                    color = getScoreColor(session.averageScore),
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = "avg",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
fun getProgressTrendColor(trend: ProgressTrend): androidx.compose.ui.graphics.Color {
    return when (trend) {
        ProgressTrend.IMPROVING -> MaterialTheme.colorScheme.primary
        ProgressTrend.STABLE -> MaterialTheme.colorScheme.outline
        ProgressTrend.DECLINING -> MaterialTheme.colorScheme.error
    }
}