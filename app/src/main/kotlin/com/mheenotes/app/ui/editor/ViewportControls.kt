package com.mheenotes.app.ui.editor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloseFullscreen
import androidx.compose.material.icons.outlined.FitScreen
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.ZoomIn
import androidx.compose.material.icons.outlined.ZoomOut
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

/**
 * Floating badge displaying current Zoom % and active Page X of Y with quick zoom actions.
 */
@Composable
fun ViewportInfoPill(
    zoomScale: Float,
    currentPageIndex: Int,
    totalPages: Int,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onFitWidth: () -> Unit,
    onFitPage: () -> Unit,
    onResetZoom: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val zoomPercentage = (zoomScale * 100).roundToInt()
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pillScale by animateFloatAsState(
        if (pressed) 0.95f else 1f,
        spring(dampingRatio = 0.6f, stiffness = 500f),
        label = "zoom-pill-scale",
    )

    Box(modifier = modifier.zIndex(42f)) {
        Surface(
            onClick = { expanded = !expanded },
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
            shadowElevation = 6.dp,
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
            ),
            interactionSource = interaction,
            modifier = Modifier.graphicsLayer { scaleX = pillScale; scaleY = pillScale },
        ) {
            Row(
                Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = "$zoomPercentage%",
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "•",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                )
                Text(
                    text = "${currentPageIndex + 1}/$totalPages",
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface),
        ) {
            DropdownMenuItem(
                text = { Text("Zoom In (+25%)") },
                leadingIcon = { Icon(Icons.Outlined.ZoomIn, null) },
                onClick = { onZoomIn(); expanded = false },
            )
            DropdownMenuItem(
                text = { Text("Zoom Out (-25%)") },
                leadingIcon = { Icon(Icons.Outlined.ZoomOut, null) },
                onClick = { onZoomOut(); expanded = false },
            )
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text("Fit Width") },
                leadingIcon = { Icon(Icons.Outlined.FitScreen, null) },
                onClick = { onFitWidth(); expanded = false },
            )
            DropdownMenuItem(
                text = { Text("Fit Page") },
                leadingIcon = { Icon(Icons.Outlined.FitScreen, null) },
                onClick = { onFitPage(); expanded = false },
            )
            DropdownMenuItem(
                text = { Text("100% (Actual Size)") },
                leadingIcon = { Icon(Icons.Outlined.RestartAlt, null) },
                onClick = { onResetZoom(); expanded = false },
            )
        }
    }
}

/**
 * Transient page change indicator capsule that appears briefly when moving between pages.
 */
@Composable
fun PageScrollBadge(
    currentPage: Int,
    totalPages: Int,
    modifier: Modifier = Modifier,
) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(currentPage) {
        visible = true
        delay(1400)
        visible = false
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(200)) + scaleIn(initialScale = 0.85f, animationSpec = tween(250, easing = FastOutSlowInEasing)) + slideInVertically { -it / 2 },
        exit = fadeOut(tween(300)) + scaleOut(targetScale = 0.9f, animationSpec = tween(300)) + slideOutVertically { -it / 2 },
        modifier = modifier.zIndex(44f),
    ) {
        Surface(
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.94f),
            shadowElevation = 8.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Page ${currentPage + 1} of $totalPages",
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * Minimal floating Zen Mode exit toggle button.
 */
@Composable
fun ZenModeFloatingToggle(
    zenMode: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = zenMode,
        enter = fadeIn(tween(220)) + scaleIn(initialScale = 0.7f),
        exit = fadeOut(tween(180)) + scaleOut(targetScale = 0.7f),
        modifier = modifier.zIndex(50f),
    ) {
        Surface(
            onClick = onToggle,
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
            shadowElevation = 6.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
            modifier = Modifier.size(42.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Outlined.CloseFullscreen,
                    contentDescription = "Exit Zen Mode",
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}
