package com.swingsync.ai.ui.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.swingsync.ai.R

sealed class Screen(
    val route: String,
    @StringRes val titleRes: Int,
    @DrawableRes val iconRes: Int
) {
    object Analysis : Screen(
        route = "analysis",
        titleRes = R.string.analysis_title,
        iconRes = R.drawable.ic_camera
    )
    
    object Coaching : Screen(
        route = "coaching",
        titleRes = R.string.coaching_title,
        iconRes = R.drawable.ic_coaching
    )
    
    object Progress : Screen(
        route = "progress",
        titleRes = R.string.progress_title,
        iconRes = R.drawable.ic_progress
    )
    
    object History : Screen(
        route = "history",
        titleRes = R.string.history_title,
        iconRes = R.drawable.ic_history
    )
    
    object Settings : Screen(
        route = "settings",
        titleRes = R.string.settings_title,
        iconRes = R.drawable.ic_settings
    )
}