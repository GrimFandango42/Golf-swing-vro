package com.swingsync.ai.ui.screens.progress

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.swingsync.ai.ui.theme.GolfGreen
import com.swingsync.ai.ui.theme.GolfGold
import com.swingsync.ai.ui.theme.GolfBlue

@Composable
fun ProgressScreen(
    navController: NavHostController,
    viewModel: ProgressViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val progressStats by viewModel.progressStats.collectAsState()
    val recentSessions by viewModel.recentSessions.collectAsState()
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Text(
                text = "Your Progress",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        
        // Progress Stats Overview
        item {
            ProgressOverviewCard(
                stats = progressStats,
                modifier = Modifier.fillMaxWidth()
            )
        }
        
        // Performance Metrics
        item {
            PerformanceMetricsCard(
                metrics = progressStats.metrics,
                modifier = Modifier.fillMaxWidth()
            )
        }
        
        // Recent Sessions
        item {
            Text(
                text = "Recent Sessions",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
        
        items(recentSessions) { session ->
            SessionCard(
                session = session,
                modifier = Modifier.fillMaxWidth()
            )
        }
        
        // Goals & Achievements
        item {
            GoalsCard(
                goals = progressStats.goals,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun ProgressOverviewCard(
    stats: ProgressStats,
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
            Text(
                text = "Progress Overview",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ProgressStatItem(
                    title = "Total Sessions",
                    value = stats.totalSessions.toString(),
                    icon = Icons.Default.Assessment,
                    color = GolfBlue
                )
                
                ProgressStatItem(
                    title = "Avg Score",
                    value = "${stats.averageScore}%",
                    icon = Icons.Default.TrendingUp,
                    color = GolfGreen
                )
                
                ProgressStatItem(
                    title = "Improvement",
                    value = "+${stats.improvement}%",
                    icon = Icons.Default.Timeline,
                    color = GolfGold
                )
            }
        }
    }
}

@Composable
fun ProgressStatItem(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(32.dp)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
        
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun PerformanceMetricsCard(
    metrics: List<PerformanceMetric>,
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
            Text(
                text = "Performance Metrics",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            metrics.forEach { metric ->
                MetricRow(
                    metric = metric,
                    modifier = Modifier.fillMaxWidth()
                )
                
                if (metric != metrics.last()) {
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
fun MetricRow(
    metric: PerformanceMetric,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = metric.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            
            Text(
                text = "${metric.currentValue}/${metric.maxValue}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        LinearProgressIndicator(
            progress = metric.progress,
            modifier = Modifier.fillMaxWidth(),
            color = when {
                metric.progress >= 0.8f -> GolfGreen
                metric.progress >= 0.6f -> GolfGold
                else -> MaterialTheme.colorScheme.primary
            }
        )
    }
}

@Composable
fun SessionCard(
    session: PracticeSession,
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = session.date,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = "${session.score}%",
                    style = MaterialTheme.typography.titleMedium,
                    color = when {
                        session.score >= 80 -> GolfGreen
                        session.score >= 60 -> GolfGold
                        else -> MaterialTheme.colorScheme.onSurface
                    },
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "${session.swingsAnalyzed} swings analyzed • ${session.duration}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            
            if (session.improvements.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Key Improvements:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                
                session.improvements.forEach { improvement ->
                    Text(
                        text = "• $improvement",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun GoalsCard(
    goals: List<Goal>,
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
            Text(
                text = "Goals & Achievements",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            goals.forEach { goal ->
                GoalItem(
                    goal = goal,
                    modifier = Modifier.fillMaxWidth()
                )
                
                if (goal != goals.last()) {
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
fun GoalItem(
    goal: Goal,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(
                    color = if (goal.isCompleted) GolfGreen else MaterialTheme.colorScheme.outline,
                    shape = RoundedCornerShape(6.dp)
                )
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = goal.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = if (goal.isCompleted) {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
            
            if (goal.description.isNotEmpty()) {
                Text(
                    text = goal.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
        
        if (goal.isCompleted) {
            Text(
                text = "✓",
                style = MaterialTheme.typography.titleMedium,
                color = GolfGreen,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// Data classes
data class ProgressStats(
    val totalSessions: Int,
    val averageScore: Int,
    val improvement: Int,
    val metrics: List<PerformanceMetric>,
    val goals: List<Goal>
)

data class PerformanceMetric(
    val name: String,
    val currentValue: Int,
    val maxValue: Int,
    val progress: Float
)

data class PracticeSession(
    val date: String,
    val score: Int,
    val swingsAnalyzed: Int,
    val duration: String,
    val improvements: List<String>
)

data class Goal(
    val title: String,
    val description: String,
    val isCompleted: Boolean
)