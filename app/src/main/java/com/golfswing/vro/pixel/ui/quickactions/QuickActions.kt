package com.golfswing.vro.pixel.ui.quickactions

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.golfswing.vro.pixel.ui.accessibility.AccessibilityUtils
import com.golfswing.vro.pixel.ui.accessibility.accessibleButton
import com.golfswing.vro.pixel.ui.accessibility.accessibleCard
import com.golfswing.vro.pixel.ui.theme.GolfTextStyles
import com.golfswing.vro.pixel.ui.theme.GolfThemeUtils

// Quick action data class
data class QuickAction(
    val id: String,
    val label: String,
    val icon: ImageVector,
    val description: String,
    val isPrimary: Boolean = false,
    val isEnabled: Boolean = true,
    val badgeCount: Int = 0
)

// Available quick actions
object QuickActions {
    val PAUSE_PRACTICE = QuickAction(
        id = "pause_practice",
        label = "Pause",
        icon = Icons.Default.Pause,
        description = "Pause current practice session",
        isPrimary = false
    )
    
    val RESUME_PRACTICE = QuickAction(
        id = "resume_practice",
        label = "Resume",
        icon = Icons.Default.PlayArrow,
        description = "Resume practice session",
        isPrimary = true
    )
    
    val REVIEW_LAST_SWING = QuickAction(
        id = "review_last_swing",
        label = "Review",
        icon = Icons.Default.Replay,
        description = "Review your last swing",
        isPrimary = false
    )
    
    val GET_TIPS = QuickAction(
        id = "get_tips",
        label = "Tips",
        icon = Icons.Default.Lightbulb,
        description = "Get coaching tips",
        isPrimary = false
    )
    
    val SAVE_SWING = QuickAction(
        id = "save_swing",
        label = "Save",
        icon = Icons.Default.Save,
        description = "Save current swing",
        isPrimary = false
    )
    
    val SHARE_PROGRESS = QuickAction(
        id = "share_progress",
        label = "Share",
        icon = Icons.Default.Share,
        description = "Share your progress",
        isPrimary = false
    )
    
    val SWITCH_MODE = QuickAction(
        id = "switch_mode",
        label = "Mode",
        icon = Icons.Default.SwapHoriz,
        description = "Switch practice mode",
        isPrimary = false
    )
    
    val VIEW_STATS = QuickAction(
        id = "view_stats",
        label = "Stats",
        icon = Icons.Default.Analytics,
        description = "View session statistics",
        isPrimary = false
    )
    
    val CAMERA_SETTINGS = QuickAction(
        id = "camera_settings",
        label = "Camera",
        icon = Icons.Default.Camera,
        description = "Camera settings",
        isPrimary = false
    )
    
    val VOICE_FEEDBACK = QuickAction(
        id = "voice_feedback",
        label = "Voice",
        icon = Icons.Default.VolumeUp,
        description = "Toggle voice feedback",
        isPrimary = false
    )
}

// Quick action bar for camera interface
@Composable
fun QuickActionBar(
    actions: List<QuickAction>,
    onActionClick: (QuickAction) -> Unit,
    modifier: Modifier = Modifier,
    isVertical: Boolean = false
) {
    Card(
        modifier = modifier
            .accessibleCard("Quick actions toolbar"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
        ),
        shape = RoundedCornerShape(GolfThemeUtils.CornerRadiusLarge.dp)
    ) {
        if (isVertical) {
            Column(
                modifier = Modifier.padding(GolfThemeUtils.SpacingSmall.dp),
                verticalArrangement = Arrangement.spacedBy(GolfThemeUtils.SpacingSmall.dp)
            ) {
                actions.forEach { action ->
                    QuickActionButton(
                        action = action,
                        onClick = { onActionClick(action) },
                        isCompact = true
                    )
                }
            }
        } else {
            LazyRow(
                modifier = Modifier.padding(GolfThemeUtils.SpacingSmall.dp),
                horizontalArrangement = Arrangement.spacedBy(GolfThemeUtils.SpacingSmall.dp),
                contentPadding = PaddingValues(horizontal = GolfThemeUtils.SpacingSmall.dp)
            ) {
                items(actions) { action ->
                    QuickActionButton(
                        action = action,
                        onClick = { onActionClick(action) },
                        isCompact = false
                    )
                }
            }
        }
    }
}

@Composable
fun QuickActionButton(
    action: QuickAction,
    onClick: () -> Unit,
    isCompact: Boolean = false,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    
    if (isCompact) {
        // Compact button for vertical layout
        FilledTonalIconButton(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            },
            enabled = action.isEnabled,
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = if (action.isPrimary) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.secondaryContainer
                },
                contentColor = if (action.isPrimary) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSecondaryContainer
                }
            ),
            modifier = modifier
                .accessibleButton(
                    contentDescription = "${action.label}. ${action.description}"
                )
        ) {
            BadgedBox(
                badge = {
                    if (action.badgeCount > 0) {
                        Badge {
                            Text(
                                text = action.badgeCount.toString(),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            ) {
                Icon(
                    imageVector = action.icon,
                    contentDescription = null
                )
            }
        }
    } else {
        // Full button with label
        Button(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            },
            enabled = action.isEnabled,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (action.isPrimary) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.secondaryContainer
                },
                contentColor = if (action.isPrimary) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSecondaryContainer
                }
            ),
            shape = RoundedCornerShape(GolfThemeUtils.CornerRadiusMedium.dp),
            modifier = modifier
                .accessibleButton(
                    contentDescription = "${action.label}. ${action.description}"
                )
        ) {
            BadgedBox(
                badge = {
                    if (action.badgeCount > 0) {
                        Badge {
                            Text(
                                text = action.badgeCount.toString(),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(GolfThemeUtils.SpacingSmall.dp)
                ) {
                    Icon(
                        imageVector = action.icon,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    
                    Text(
                        text = action.label,
                        style = GolfTextStyles.QuickActionLabel
                    )
                }
            }
        }
    }
}

// Floating action button for primary actions
@Composable
fun PrimaryQuickActionFAB(
    action: QuickAction,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    
    FloatingActionButton(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        },
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        modifier = modifier
            .size(64.dp)
            .accessibleButton(
                contentDescription = "${action.label}. ${action.description}"
            )
    ) {
        BadgedBox(
            badge = {
                if (action.badgeCount > 0) {
                    Badge(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ) {
                        Text(
                            text = action.badgeCount.toString(),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        ) {
            Icon(
                imageVector = action.icon,
                contentDescription = null,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

// Quick action grid for settings/menu
@Composable
fun QuickActionGrid(
    actions: List<QuickAction>,
    onActionClick: (QuickAction) -> Unit,
    modifier: Modifier = Modifier,
    columns: Int = 3
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = modifier,
        contentPadding = PaddingValues(GolfThemeUtils.SpacingMedium.dp),
        verticalArrangement = Arrangement.spacedBy(GolfThemeUtils.SpacingMedium.dp),
        horizontalArrangement = Arrangement.spacedBy(GolfThemeUtils.SpacingMedium.dp)
    ) {
        items(actions.size) { index ->
            val action = actions[index]
            QuickActionGridItem(
                action = action,
                onClick = { onActionClick(action) }
            )
        }
    }
}

@Composable
fun QuickActionGridItem(
    action: QuickAction,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    
    Card(
        modifier = modifier
            .aspectRatio(1f)
            .accessibleCard("${action.label}. ${action.description}"),
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        },
        enabled = action.isEnabled,
        colors = CardDefaults.cardColors(
            containerColor = if (action.isPrimary) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainer
            }
        ),
        shape = RoundedCornerShape(GolfThemeUtils.CornerRadiusMedium.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(GolfThemeUtils.SpacingMedium.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            BadgedBox(
                badge = {
                    if (action.badgeCount > 0) {
                        Badge(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        ) {
                            Text(
                                text = action.badgeCount.toString(),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            ) {
                Icon(
                    imageVector = action.icon,
                    contentDescription = null,
                    tint = if (action.isPrimary) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    modifier = Modifier.size(28.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(GolfThemeUtils.SpacingSmall.dp))
            
            Text(
                text = action.label,
                style = GolfTextStyles.QuickActionLabel,
                color = if (action.isPrimary) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// Context-aware quick actions based on current state
@Composable
fun ContextualQuickActions(
    isRecording: Boolean,
    isPaused: Boolean,
    hasLastSwing: Boolean,
    onActionClick: (QuickAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val contextualActions = remember(isRecording, isPaused, hasLastSwing) {
        buildList {
            if (isRecording) {
                add(QuickActions.PAUSE_PRACTICE)
                add(QuickActions.SAVE_SWING)
                add(QuickActions.CAMERA_SETTINGS)
            } else if (isPaused) {
                add(QuickActions.RESUME_PRACTICE.copy(isPrimary = true))
                add(QuickActions.SWITCH_MODE)
                add(QuickActions.VIEW_STATS)
            } else {
                add(QuickActions.SWITCH_MODE)
                add(QuickActions.GET_TIPS)
                add(QuickActions.CAMERA_SETTINGS)
            }
            
            if (hasLastSwing) {
                add(QuickActions.REVIEW_LAST_SWING)
            }
            
            add(QuickActions.VOICE_FEEDBACK)
        }
    }
    
    QuickActionBar(
        actions = contextualActions,
        onActionClick = onActionClick,
        modifier = modifier
    )
}

// Extension function to create custom quick actions
fun QuickAction.withBadge(count: Int): QuickAction {
    return this.copy(badgeCount = count)
}

fun QuickAction.asPrimary(): QuickAction {
    return this.copy(isPrimary = true)
}

fun QuickAction.disabled(): QuickAction {
    return this.copy(isEnabled = false)
}