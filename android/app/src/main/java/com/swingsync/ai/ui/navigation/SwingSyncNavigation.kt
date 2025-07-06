package com.swingsync.ai.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.swingsync.ai.R
import com.swingsync.ai.ui.screens.analysis.AnalysisScreen
import com.swingsync.ai.ui.screens.coaching.CoachingScreen
import com.swingsync.ai.ui.screens.history.HistoryScreen
import com.swingsync.ai.ui.screens.progress.ProgressScreen
import com.swingsync.ai.ui.screens.settings.SettingsScreen

@Composable
fun SwingSyncNavigation(
    navController: NavHostController = rememberNavController()
) {
    Scaffold(
        bottomBar = {
            SwingSyncBottomNavigation(navController = navController)
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Analysis.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(Screen.Analysis.route) {
                AnalysisScreen(navController = navController)
            }
            composable(Screen.Coaching.route) {
                CoachingScreen(navController = navController)
            }
            composable(Screen.Progress.route) {
                ProgressScreen(navController = navController)
            }
            composable(Screen.History.route) {
                HistoryScreen(navController = navController)
            }
            composable(Screen.Settings.route) {
                SettingsScreen(navController = navController)
            }
        }
    }
}

@Composable
fun SwingSyncBottomNavigation(
    navController: NavHostController
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    NavigationBar {
        bottomNavItems.forEach { screen ->
            NavigationBarItem(
                icon = {
                    Icon(
                        painter = painterResource(id = screen.iconRes),
                        contentDescription = null
                    )
                },
                label = {
                    Text(stringResource(screen.titleRes))
                },
                selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                onClick = {
                    navController.navigate(screen.route) {
                        // Pop up to the start destination of the graph to
                        // avoid building up a large stack of destinations
                        // on the back stack as users select items
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        // Avoid multiple copies of the same destination when
                        // reselecting the same item
                        launchSingleTop = true
                        // Restore state when reselecting a previously selected item
                        restoreState = true
                    }
                }
            )
        }
    }
}

val bottomNavItems = listOf(
    Screen.Analysis,
    Screen.Coaching,
    Screen.Progress,
    Screen.History,
    Screen.Settings
)