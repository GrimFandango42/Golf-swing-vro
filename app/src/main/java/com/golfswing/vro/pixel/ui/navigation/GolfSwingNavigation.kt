package com.golfswing.vro.pixel.ui.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.golfswing.vro.pixel.coaching.RealTimeCoachingEngine
import com.golfswing.vro.pixel.ui.camera.CameraScreen
import com.golfswing.vro.pixel.ui.personal.PersonalDashboard
import com.golfswing.vro.pixel.ui.personal.PersonalProgress
import com.golfswing.vro.pixel.ui.personal.PersonalSession
import com.golfswing.vro.pixel.ui.personal.PersonalPreferences
import com.golfswing.vro.pixel.ui.personal.ProgressTrend
import com.golfswing.vro.pixel.ui.accessibility.AccessibilityUtils
import com.golfswing.vro.pixel.ui.accessibility.accessibleButton
import com.golfswing.vro.pixel.ui.theme.GolfThemeUtils
import java.time.LocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GolfSwingNavigation() {
    val navController = rememberNavController()
    var selectedItem by remember { mutableStateOf(0) }
    
    // Sample data for demonstration
    val sampleProgress = PersonalProgress(
        totalSessions = 25,
        totalSwings = 1250,
        averageScore = 6.8f,
        bestScore = 8.5f,
        consistencyImprovement = 0.15f,
        streakDays = 7,
        favoriteMode = "full_swing",
        recentTrend = ProgressTrend.IMPROVING,
        weeklyGoalProgress = 0.6f
    )
    
    val sampleSessions = listOf(
        PersonalSession(
            id = "1",
            timestamp = LocalDateTime.now().minusDays(1),
            practiceMode = "full_swing",
            swingCount = 45,
            averageScore = 7.2f,
            bestScore = 8.1f,
            consistencyScore = 0.75f,
            totalDuration = 22,
            improvementAreas = listOf("Follow-through", "Tempo")
        ),
        PersonalSession(
            id = "2",
            timestamp = LocalDateTime.now().minusDays(2),
            practiceMode = "putting",
            swingCount = 30,
            averageScore = 6.9f,
            bestScore = 7.8f,
            consistencyScore = 0.82f,
            totalDuration = 18,
            improvementAreas = listOf("Alignment", "Distance control")
        )
    )
    
    val samplePreferences = PersonalPreferences()
    
    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { 
                        Icon(
                            Icons.Default.Home, 
                            contentDescription = "Home dashboard"
                        ) 
                    },
                    label = { Text("Home") },
                    selected = selectedItem == 0,
                    onClick = { 
                        selectedItem = 0
                        navController.navigate("home")
                    },
                    modifier = Modifier.accessibleButton(
                        "Home dashboard. View your personal practice overview and progress."
                    )
                )
                
                NavigationBarItem(
                    icon = { 
                        Icon(
                            Icons.Default.Camera, 
                            contentDescription = "Camera practice"
                        ) 
                    },
                    label = { Text("Practice") },
                    selected = selectedItem == 1,
                    onClick = { 
                        selectedItem = 1
                        navController.navigate("camera")
                    },
                    modifier = Modifier.accessibleButton(
                        "Camera practice. Start a new practice session with real-time feedback."
                    )
                )
                
                NavigationBarItem(
                    icon = { 
                        Icon(
                            Icons.Default.Person, 
                            contentDescription = "Profile"
                        ) 
                    },
                    label = { Text("Profile") },
                    selected = selectedItem == 2,
                    onClick = { 
                        selectedItem = 2
                        navController.navigate("profile")
                    },
                    modifier = Modifier.accessibleButton(
                        "Profile. View and edit your personal preferences and settings."
                    )
                )
                
                NavigationBarItem(
                    icon = { 
                        Icon(
                            Icons.Default.Settings, 
                            contentDescription = "Settings"
                        ) 
                    },
                    label = { Text("Settings") },
                    selected = selectedItem == 3,
                    onClick = { 
                        selectedItem = 3
                        navController.navigate("settings")
                    },
                    modifier = Modifier.accessibleButton(
                        "Settings. Configure app preferences and accessibility options."
                    )
                )
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(paddingValues)
        ) {
            composable("home") {
                PersonalDashboard(
                    progress = sampleProgress,
                    recentSessions = sampleSessions,
                    preferences = samplePreferences,
                    onStartQuickPractice = {
                        selectedItem = 1
                        navController.navigate("camera")
                    },
                    onViewProgress = {
                        // Navigate to detailed progress view
                    },
                    onEditPreferences = {
                        selectedItem = 2
                        navController.navigate("profile")
                    }
                )
            }
            
            composable("camera") {
                CameraScreen()
            }
            
            composable("profile") {
                ProfileScreen()
            }
            
            composable("settings") {
                SettingsScreen()
            }
        }
    }
}

@Composable
fun ProfileScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(GolfThemeUtils.SpacingMedium.dp)
    ) {
        Text(
            text = "Profile & Preferences",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(GolfThemeUtils.SpacingMedium.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(GolfThemeUtils.SpacingMedium.dp)
            ) {
                Text(
                    text = "Coming Soon",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(GolfThemeUtils.SpacingSmall.dp))
                
                Text(
                    text = "Personal preferences, swing history, and detailed analytics will be available here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun SettingsScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(GolfThemeUtils.SpacingMedium.dp)
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(GolfThemeUtils.SpacingMedium.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(GolfThemeUtils.SpacingMedium.dp)
            ) {
                Text(
                    text = "App Configuration",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(GolfThemeUtils.SpacingSmall.dp))
                
                Text(
                    text = "Camera settings, accessibility options, coaching preferences, and data management will be available here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}