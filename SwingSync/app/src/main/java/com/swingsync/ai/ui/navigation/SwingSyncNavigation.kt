package com.swingsync.ai.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.swingsync.ai.ui.screens.*

@Composable
fun SwingSyncNavigation(
    navController: NavHostController,
    startDestination: String = "home"
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Onboarding
        composable("onboarding") {
            OnboardingScreen(
                onComplete = {
                    navController.navigate("home") {
                        popUpTo("onboarding") { inclusive = true }
                    }
                }
            )
        }
        
        // Home
        composable("home") {
            HomeScreen(
                onNavigateToCamera = { navController.navigate("camera") },
                onNavigateToAnalysis = { navController.navigate("analysis") },
                onNavigateToProfile = { navController.navigate("profile") },
                onNavigateToSettings = { navController.navigate("settings") }
            )
        }
        
        // Camera
        composable("camera") {
            CameraScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToAnalysis = { analysisId ->
                    navController.navigate("analysis/$analysisId")
                }
            )
        }
        
        // Analysis
        composable("analysis") {
            AnalysisScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToCamera = { navController.navigate("camera") }
            )
        }
        
        composable("analysis/{analysisId}") { backStackEntry ->
            val analysisId = backStackEntry.arguments?.getString("analysisId") ?: ""
            AnalysisDetailScreen(
                analysisId = analysisId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        // Profile
        composable("profile") {
            ProfileScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToSettings = { navController.navigate("settings") }
            )
        }
        
        // Settings
        composable("settings") {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}