package com.mheenotes.ui.design

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import kotlinx.coroutines.delay

// ---- Host ----------------------------------------------------------------------------------

internal class ModalEntry(val key: Any, content: @Composable () -> Unit) {
    var content by mutableStateOf(content)
    /** False while the modal plays its exit animation, so the backdrop can clear immediately. */
    var visible by mutableStateOf(true)
}

@Stable
class OverlayController {
    internal val modals = mutableStateListOf<ModalEntry>()
    internal var toast by mutableStateOf<Pair<Long, String>?>(null)
    private var toastSeq = 0L

    fun toast(message: String) {
        toast = (++toastSeq) to message
    }
}

val LocalOverlays = staticCompositionLocalOf<OverlayController> { error("OverlayHost missing") }

/**
 * Root host for modals and toasts. Modals render in-tree, so the app behind them can blur.
 */
@Composable
fun OverlayHost(content: @Composable () -> Unit) {
    val controller = remember { OverlayController() }
    val blurred = controller.modals.any { it.visible }
    val blur by animateFloatAsState(if (blurred) 1f else 0f, androidx.compose.animation.core.tween(160), label = "blur")
    CompositionLocalProvider(LocalOverlays provides controller) {
        Box(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize().then(if (blur > 0.01f) Modifier.blur((10 * blur).dp) else Modifier)) { content() }
            controller.modals.forEach { entry -> androidx.compose.runtime.key(entry.key) { entry.content() } }
            ToastLayer(controller)
        }
    }
}

@Composable
private fun BoxScope.ToastLayer(controller: OverlayController) {
    val current = controller.toast
    var visible by remember { mutableStateOf(false) }
    var text by remember { mutableStateOf("") }
    LaunchedEffect(current) {
        if (current != null) {
            text = current.second
            visible = true
            delay(2600)
            visible = false
        }
    }
    AnimatedVisibility(
        visible,
        Modifier.align(Alignment.BottomCenter).padding(bottom = 36.dp),
        enter = slideInVertically(Motion.bouncy()) { it } + fadeIn(Motion.fade()),
        exit = slideOutVertically(Motion.standard()) { it } + fadeOut(Motion.fade()),
    ) {
        val c = M.colors
        Card(color = if (c.isDark) c.elevated else Color(0xF21C1C1F), elevation = 12.dp, border = false, shape = RoundedCornerShape(50)) {
            Text(text, Modifier.padding(horizontal = 20.dp, vertical = 12.dp), style = M.type.calloutStrong, color = Color.White)
        }
    }
}

// ---- Modal ---------------------------------------------------------------------------------

/**
 * Centered dialog card with a dimmed, blurred backdrop. Stays composed through its exit
 * animation, so callers just flip [visible].
 */
@Composable
fun Modal(
    visible: Boolean,
    onDismiss: () -> Unit,
    width: Dp = 460.dp,
    dismissOnScrim: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val controller = LocalOverlays.current
    val state = remember { MutableTransitionState(false) }
    state.targetState = visible
    val key = remember { Any() }
    val show = visible || !state.isIdle || state.currentState
    val body: @Composable () -> Unit = {
        PlatformBackHandler(visible) { onDismiss() }
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            AnimatedVisibility(state, enter = fadeIn(Motion.fade()), exit = fadeOut(Motion.fade())) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(M.colors.scrim)
                        .clickable(remember { MutableInteractionSource() }, null) { if (dismissOnScrim) onDismiss() },
                )
            }
            AnimatedVisibility(
                state,
                Modifier.imePadding(),
                enter = scaleIn(Motion.bouncy(), initialScale = 0.9f) + fadeIn(Motion.fade()),
                exit = scaleOut(Motion.snappy(), targetScale = 0.95f) + fadeOut(Motion.fade()),
            ) {
                Card(
                    Modifier.padding(24.dp).widthIn(max = width).fillMaxWidth()
                        .clickable(remember { MutableInteractionSource() }, null) {},
                    shape = RoundedCornerShape(Radii.xl),
                    color = M.colors.elevated,
                    elevation = 28.dp,
                ) { Column(Modifier.padding(24.dp), content = content) }
            }
        }
    }
    if (show) {
        val entry = remember { ModalEntry(key, body) }
        SideEffect { entry.content = body; entry.visible = visible }
        DisposableEffect(Unit) {
            controller.modals += entry
            onDispose { controller.modals -= entry }
        }
    }
}

@Composable
fun ModalTitle(title: String, subtitle: String? = null) {
    Text(title, style = M.type.title)
    if (subtitle != null) {
        Spacer(Modifier.height(6.dp))
        Text(subtitle, style = M.type.callout, color = M.colors.textSecondary)
    }
    Spacer(Modifier.height(20.dp))
}

@Composable
fun ModalActions(content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) {
    Spacer(Modifier.height(24.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp, Alignment.End), content = content)
}

/** Yes/no confirmation built on [Modal]. */
@Composable
fun ConfirmModal(
    visible: Boolean,
    title: String,
    message: String,
    confirm: String,
    cancel: String,
    danger: Boolean = false,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    Modal(visible, onDismiss, width = 400.dp) {
        ModalTitle(title, message)
        ModalActions {
            Button(cancel, onDismiss, style = ButtonStyle.Secondary)
            Button(confirm, { onConfirm(); onDismiss() }, style = if (danger) ButtonStyle.Danger else ButtonStyle.Primary)
        }
    }
}

// ---- Popover menus -------------------------------------------------------------------------

enum class PopoverSide { Below, Above, End, Start }

private class AnchorProvider(val side: PopoverSide, val gap: Int, val align: Alignment.Horizontal) : PopupPositionProvider {
    override fun calculatePosition(anchorBounds: IntRect, windowSize: IntSize, layoutDirection: LayoutDirection, popupContentSize: IntSize): IntOffset {
        val w = popupContentSize.width
        val h = popupContentSize.height
        var x = when (side) {
            PopoverSide.End -> anchorBounds.right + gap
            PopoverSide.Start -> anchorBounds.left - gap - w
            else -> when (align) {
                Alignment.Start -> anchorBounds.left
                Alignment.End -> anchorBounds.right - w
                else -> anchorBounds.center.x - w / 2
            }
        }
        var y = when (side) {
            PopoverSide.Below -> anchorBounds.bottom + gap
            PopoverSide.Above -> anchorBounds.top - gap - h
            else -> anchorBounds.top
        }
        if (side == PopoverSide.Below && y + h > windowSize.height) y = anchorBounds.top - gap - h
        if (side == PopoverSide.Above && y < 0) y = anchorBounds.bottom + gap
        x = x.coerceIn(gap, (windowSize.width - w - gap).coerceAtLeast(gap))
        y = y.coerceIn(gap, (windowSize.height - h - gap).coerceAtLeast(gap))
        return IntOffset(x, y)
    }
}

/**
 * Anchored floating panel. Place it inside the anchor's Box; it springs out of the anchor.
 */
@Composable
fun Popover(
    expanded: Boolean,
    onDismiss: () -> Unit,
    side: PopoverSide = PopoverSide.Below,
    align: Alignment.Horizontal = Alignment.CenterHorizontally,
    width: Dp? = null,
    focusable: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val state = remember { MutableTransitionState(false) }
    state.targetState = expanded
    if (!expanded && state.isIdle && !state.currentState) return
    val gap = with(androidx.compose.ui.platform.LocalDensity.current) { 8.dp.roundToPx() }
    val origin = when (side) {
        PopoverSide.Below -> TransformOrigin(0.5f, 0f)
        PopoverSide.Above -> TransformOrigin(0.5f, 1f)
        PopoverSide.End -> TransformOrigin(0f, 0.1f)
        PopoverSide.Start -> TransformOrigin(1f, 0.1f)
    }
    Popup(
        popupPositionProvider = remember(side, gap, align) { AnchorProvider(side, gap, align) },
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = focusable),
    ) {
        AnimatedVisibility(
            state,
            enter = scaleIn(Motion.bouncy(), initialScale = 0.85f, transformOrigin = origin) + fadeIn(Motion.fade()),
            exit = scaleOut(Motion.snappy(), targetScale = 0.92f, transformOrigin = origin) + fadeOut(Motion.fade()),
        ) {
            Card(
                Modifier.padding(16.dp).then(if (width != null) Modifier.width(width) else Modifier.widthIn(min = 200.dp, max = 320.dp)),
                shape = RoundedCornerShape(Radii.lg),
                color = M.colors.elevated,
                elevation = 18.dp,
            ) { Column(Modifier.padding(6.dp), content = content) }
        }
    }
}

@Composable
fun MenuItem(
    text: String,
    icon: ImageVector? = null,
    danger: Boolean = false,
    enabled: Boolean = true,
    trailing: String? = null,
    onClick: () -> Unit,
) {
    val c = M.colors
    val fg = if (danger) c.danger else c.text
    Row(
        Modifier
            .fillMaxWidth()
            .graphicsLayer { alpha = if (enabled) 1f else 0.35f }
            .pressable(enabled, pressedScale = 0.97f, onClick = onClick)
            .heightIn(min = 42.dp)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, tint = fg, size = 18.dp)
            Spacer(Modifier.width(12.dp))
        }
        Text(text, Modifier.weight(1f), style = M.type.body, color = fg, maxLines = 1)
        if (trailing != null) Text(trailing, style = M.type.callout, color = c.textTertiary)
    }
}

@Composable
fun MenuDivider() = Divider(Modifier.padding(vertical = 4.dp, horizontal = 8.dp))

@Composable
fun MenuLabel(text: String) = Text(text.uppercase(), Modifier.padding(start = 12.dp, top = 8.dp, bottom = 4.dp), style = M.type.overline, color = M.colors.textTertiary)

@Composable
expect fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit)
