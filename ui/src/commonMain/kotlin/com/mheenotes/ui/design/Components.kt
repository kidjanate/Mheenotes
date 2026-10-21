package com.mheenotes.ui.design

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.X
import kotlin.math.roundToInt

// ---- Text & icons ---------------------------------------------------------------------------

@Composable
fun Text(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = M.type.body,
    color: Color = M.colors.text,
    maxLines: Int = Int.MAX_VALUE,
) {
    BasicText(
        text = text,
        modifier = modifier,
        style = style.copy(color = color),
        maxLines = maxLines,
        overflow = if (maxLines == Int.MAX_VALUE) TextOverflow.Clip else TextOverflow.Ellipsis,
    )
}

@Composable
fun Icon(icon: ImageVector, modifier: Modifier = Modifier, tint: Color = M.colors.text, size: Dp = 20.dp) {
    Image(icon, contentDescription = null, modifier = modifier.size(size), colorFilter = ColorFilter.tint(tint))
}

// ---- Press feedback ------------------------------------------------------------------------

/**
 * Click with the house press feel: a soft spring scale instead of a ripple.
 */
fun Modifier.pressable(
    enabled: Boolean = true,
    pressedScale: Float = 0.95f,
    role: Role = Role.Button,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit,
): Modifier = composed {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed && enabled) pressedScale else 1f, Motion.snappy(), label = "press")
    this
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .then(
            if (onLongClick == null) {
                Modifier.clickable(source, indication = null, enabled = enabled, role = role, onClick = onClick)
            } else {
                Modifier.pointerInput(enabled) {
                    if (enabled) detectTapGestures(
                        onPress = {
                            val press = androidx.compose.foundation.interaction.PressInteraction.Press(it)
                            source.emit(press)
                            val released = tryAwaitRelease()
                            source.emit(
                                if (released) androidx.compose.foundation.interaction.PressInteraction.Release(press)
                                else androidx.compose.foundation.interaction.PressInteraction.Cancel(press),
                            )
                        },
                        onTap = { onClick() },
                        onLongPress = { onLongClick() },
                    )
                }
            },
        )
}

// ---- Surfaces ------------------------------------------------------------------------------

@Composable
fun Card(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(Radii.lg),
    color: Color = M.colors.surface,
    elevation: Dp = 0.dp,
    border: Boolean = true,
    content: @Composable () -> Unit,
) {
    val c = M.colors
    Box(
        modifier
            .then(if (elevation > 0.dp) Modifier.shadow(elevation, shape, ambientColor = c.shadow, spotColor = c.shadow) else Modifier)
            .clip(shape)
            .background(color)
            .then(if (border) Modifier.border(0.75.dp, c.separator, shape) else Modifier),
    ) { content() }
}

@Composable
fun Divider(modifier: Modifier = Modifier, vertical: Boolean = false) {
    val c = M.colors.separator
    Box(
        if (vertical) modifier.width(0.75.dp).fillMaxHeight().background(c)
        else modifier.height(0.75.dp).fillMaxWidth().background(c),
    )
}

// ---- Buttons -------------------------------------------------------------------------------

enum class ButtonStyle { Primary, Secondary, Ghost, Danger }

@Composable
fun Button(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    style: ButtonStyle = ButtonStyle.Secondary,
    enabled: Boolean = true,
    compact: Boolean = false,
) {
    val c = M.colors
    val (bg, fg) = when (style) {
        ButtonStyle.Primary -> c.accent to c.onAccent
        ButtonStyle.Secondary -> c.fill to c.text
        ButtonStyle.Ghost -> Color.Transparent to c.accent
        ButtonStyle.Danger -> c.dangerSoft to c.danger
    }
    val alpha by animateFloatAsState(if (enabled) 1f else 0.4f, Motion.fade(), label = "enabled")
    Row(
        modifier
            .graphicsLayer { this.alpha = alpha }
            .pressable(enabled, onClick = onClick)
            .heightIn(min = if (compact) 32.dp else 42.dp)
            .clip(RoundedCornerShape(Radii.md))
            .background(bg)
            .padding(horizontal = if (compact) 12.dp else 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) {
            Icon(icon, tint = fg, size = if (compact) 16.dp else 18.dp)
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = if (compact) M.type.calloutStrong else M.type.bodyStrong, color = fg, maxLines = 1)
    }
}

@Composable
fun IconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    enabled: Boolean = true,
    tint: Color? = null,
    size: Dp = 40.dp,
    iconSize: Dp = 20.dp,
    onLongClick: (() -> Unit)? = null,
) {
    val c = M.colors
    val bg by animateColorAsState(if (selected) c.accentSoft else Color.Transparent, Motion.fade(), label = "bg")
    val fg = tint ?: if (selected) c.accent else c.text
    val alpha by animateFloatAsState(if (enabled) 1f else 0.3f, Motion.fade(), label = "enabled")
    Box(
        modifier
            .graphicsLayer { this.alpha = alpha }
            .pressable(enabled, pressedScale = 0.88f, onLongClick = onLongClick, onClick = onClick)
            .size(size)
            .clip(RoundedCornerShape(Radii.sm))
            .background(bg),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, tint = fg, size = iconSize) }
}

// ---- Inputs --------------------------------------------------------------------------------

@Composable
fun TextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    leading: ImageVector? = null,
    clearable: Boolean = false,
    singleLine: Boolean = true,
    onSubmit: (() -> Unit)? = null,
    style: TextStyle = M.type.body,
) {
    val c = M.colors
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = singleLine,
        textStyle = style.copy(color = c.text),
        cursorBrush = SolidColor(c.accent),
        keyboardOptions = KeyboardOptions(imeAction = if (onSubmit != null) ImeAction.Done else ImeAction.Default),
        keyboardActions = KeyboardActions(onDone = { onSubmit?.invoke() }),
        modifier = modifier,
        decorationBox = { inner ->
            Row(
                Modifier
                    .clip(RoundedCornerShape(Radii.md))
                    .background(c.fill)
                    .heightIn(min = 42.dp)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (leading != null) {
                    Icon(leading, tint = c.textSecondary, size = 18.dp)
                    Spacer(Modifier.width(8.dp))
                }
                Box(Modifier.weight(1f).padding(vertical = 10.dp)) {
                    if (value.isEmpty()) Text(placeholder, style = style, color = c.textTertiary, maxLines = 1)
                    inner()
                }
                if (clearable && value.isNotEmpty()) {
                    Box(
                        Modifier.pressable(onClick = { onValueChange("") }).size(22.dp).clip(CircleShape).background(c.fillStrong),
                        contentAlignment = Alignment.Center,
                    ) { Icon(Lucide.X, tint = c.textSecondary, size = 13.dp) }
                }
            }
        },
    )
}

@Composable
fun Toggle(checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val c = M.colors
    val track by animateColorAsState(if (checked) c.accent else c.fillStrong, Motion.fade(), label = "track")
    val x by animateDpAsState(if (checked) 20.dp else 2.dp, Motion.bouncy(), label = "thumb")
    Box(
        modifier
            .pressable(pressedScale = 0.94f, role = Role.Switch) { onCheckedChange(!checked) }
            .size(width = 46.dp, height = 28.dp)
            .clip(CircleShape)
            .background(track),
    ) {
        Box(
            Modifier
                .offset(x = x, y = 2.dp)
                .size(24.dp)
                .shadow(2.dp, CircleShape)
                .clip(CircleShape)
                .background(Color.White),
        )
    }
}

@Composable
fun Checkbox(checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val c = M.colors
    val bg by animateColorAsState(if (checked) c.accent else Color.Transparent, Motion.fade(), label = "bg")
    val scale by animateFloatAsState(if (checked) 1f else 0.4f, Motion.bouncy(), label = "check")
    Box(
        modifier
            .pressable(role = Role.Checkbox) { onCheckedChange(!checked) }
            .size(24.dp)
            .clip(CircleShape)
            .background(bg)
            .border(1.5.dp, if (checked) c.accent else c.textTertiary, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) Icon(Lucide.Check, Modifier.graphicsLayer { scaleX = scale; scaleY = scale }, tint = c.onAccent, size = 15.dp)
    }
}

/** iOS-style segmented control with a sliding thumb. */
@Composable
fun <T> Segmented(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    label: @Composable (T, Boolean) -> Unit,
) {
    val c = M.colors
    BoxWithConstraints(
        modifier
            .height(36.dp)
            .clip(RoundedCornerShape(Radii.sm))
            .background(c.fill)
            .padding(3.dp),
    ) {
        val w = maxWidth / options.size
        val index = options.indexOf(selected).coerceAtLeast(0)
        val x by animateDpAsState(w * index, Motion.standard(), label = "seg")
        Box(
            Modifier
                .offset(x = x)
                .width(w)
                .fillMaxHeight()
                .shadow(1.5.dp, RoundedCornerShape(Radii.sm - 3.dp), ambientColor = c.shadow, spotColor = c.shadow)
                .clip(RoundedCornerShape(Radii.sm - 3.dp))
                .background(if (c.isDark) c.fillStrong else c.surface),
        )
        Row(Modifier.fillMaxWidth().fillMaxHeight()) {
            options.forEach { o ->
                Box(
                    Modifier.weight(1f).fillMaxHeight().clickable(remember { MutableInteractionSource() }, null) { onSelect(o) },
                    contentAlignment = Alignment.Center,
                ) { label(o, o == selected) }
            }
        }
    }
}

@Composable
fun Slider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    range: ClosedFloatingPointRange<Float> = 0f..1f,
    onChangeFinished: () -> Unit = {},
) {
    val c = M.colors
    val density = LocalDensity.current
    var widthPx by remember { mutableFloatStateOf(1f) }
    val frac = ((value - range.start) / (range.endInclusive - range.start)).coerceIn(0f, 1f)
    fun set(x: Float) = onValueChange(range.start + (x / widthPx).coerceIn(0f, 1f) * (range.endInclusive - range.start))
    Box(
        modifier
            .height(28.dp)
            .pointerInput(range) {
                widthPx = size.width.toFloat()
                detectTapGestures { set(it.x); onChangeFinished() }
            }
            .pointerInput(range) {
                detectHorizontalDragGestures(onDragEnd = onChangeFinished) { ch, _ -> set(ch.position.x) }
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(Modifier.fillMaxWidth().height(4.dp).clip(CircleShape).background(c.fillStrong))
        Box(Modifier.fillMaxWidth(frac).height(4.dp).clip(CircleShape).background(c.accent))
        val thumb = 22.dp
        Box(
            Modifier
                .offset { IntOffset(((widthPx - with(density) { thumb.toPx() }) * frac).roundToInt(), 0) }
                .size(thumb)
                .shadow(3.dp, CircleShape)
                .clip(CircleShape)
                .background(Color.White),
        )
    }
}

@Composable
fun Chip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    val c = M.colors
    val bg by animateColorAsState(if (selected) c.text else c.fill, Motion.fade(), label = "chip")
    val fg = if (selected) c.background else c.text
    Row(
        modifier.pressable(onClick = onClick).height(32.dp).clip(CircleShape).background(bg).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) { Icon(icon, tint = fg, size = 15.dp); Spacer(Modifier.width(6.dp)) }
        Text(text, style = M.type.calloutStrong, color = fg, maxLines = 1)
    }
}

@Composable
fun Spinner(modifier: Modifier = Modifier, size: Dp = 22.dp, color: Color = M.colors.textSecondary) {
    val t = rememberInfiniteTransition(label = "spin")
    val angle by t.animateFloat(0f, 360f, infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Restart), label = "a")
    Canvas(modifier.size(size)) {
        val w = this.size.minDimension * 0.12f
        drawArc(color, angle, 270f, false, topLeft = Offset(w, w), size = androidx.compose.ui.geometry.Size(this.size.width - 2 * w, this.size.height - 2 * w), style = Stroke(w, cap = StrokeCap.Round))
    }
}

/** A row inside a grouped settings card. */
@Composable
fun ListRow(
    title: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    val c = M.colors
    Row(
        modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.pressable(pressedScale = 0.985f, onClick = onClick) else Modifier)
            .heightIn(min = 52.dp)
            .padding(PaddingValues(horizontal = 16.dp, vertical = 8.dp)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, tint = c.textSecondary, size = 19.dp)
            Spacer(Modifier.width(14.dp))
        }
        androidx.compose.foundation.layout.Column(Modifier.weight(1f)) {
            Text(title, style = M.type.body, maxLines = 1)
            if (subtitle != null) Text(subtitle, style = M.type.callout, color = c.textSecondary, maxLines = 2)
        }
        trailing()
    }
}

@Composable
fun ColorDot(
    color: Color,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 30.dp,
    onLongClick: (() -> Unit)? = null,
) {
    val c = M.colors
    val ring by animateFloatAsState(if (selected) 1f else 0f, Motion.bouncy(), label = "ring")
    Box(modifier.pressable(pressedScale = 0.85f, onLongClick = onLongClick, onClick = onClick).size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val r = this.size.minDimension / 2
            if (ring > 0f) drawCircle(c.accent, r, style = Stroke(2.dp.toPx() * ring))
            drawCircle(color, r - 4.dp.toPx() * ring.coerceAtLeast(0.001f) - 1.dp.toPx())
            drawCircle(c.separator, r - 4.dp.toPx() * ring - 1.dp.toPx(), style = Stroke(0.75.dp.toPx()))
        }
    }
}
