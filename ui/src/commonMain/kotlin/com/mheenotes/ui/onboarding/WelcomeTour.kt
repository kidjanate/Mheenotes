package com.mheenotes.ui.onboarding

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.FileUp
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.PenLine
import com.composables.icons.lucide.Redo2
import com.composables.icons.lucide.Undo2
import com.mheenotes.ui.design.Button
import com.mheenotes.ui.design.ButtonStyle
import com.mheenotes.ui.design.Icon
import com.mheenotes.ui.design.M
import com.mheenotes.ui.design.Modal
import com.mheenotes.ui.design.Radii
import com.mheenotes.ui.design.Text
import com.mheenotes.ui.i18n.S
import com.mheenotes.ui.library.LanguagePicker
import com.mheenotes.ui.library.ThemePicker
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

private const val PAGES = 6

/** First-run tour: language, theme, then the essentials, each page with a small looping animation. */
@Composable
fun WelcomeTour(visible: Boolean, onImportPdf: () -> Unit, onDone: () -> Unit) {
    val s = S
    val c = M.colors
    val pager = rememberPagerState { PAGES }
    val scope = rememberCoroutineScope()
    LaunchedEffect(visible) { if (visible) pager.scrollToPage(0) }
    val last = pager.currentPage == PAGES - 1
    Modal(visible, onDone, width = 600.dp, dismissOnScrim = false) {
        HorizontalPager(pager, Modifier.fillMaxWidth().height(410.dp), pageSpacing = 24.dp, verticalAlignment = Alignment.Top) { page ->
            TourPage(page, pager, onImportPdf)
        }
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) {
                if (!last) Button(s.tourSkip, onDone, style = ButtonStyle.Ghost, compact = true)
            }
            Dots(pager)
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                if (pager.currentPage > 0) Button(s.tourBack, { scope.launch { pager.animateScrollToPage(pager.currentPage - 1) } }, style = ButtonStyle.Ghost, compact = true)
                if (last) Button(s.tourStart, onDone, style = ButtonStyle.Primary, compact = true)
                else Button(s.tourNext, { scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } }, style = ButtonStyle.Primary, compact = true)
            }
        }
    }
}

@Composable
private fun TourPage(page: Int, pager: PagerState, onImportPdf: () -> Unit) {
    val s = S
    val c = M.colors
    // How far this page is from the centre, for a gentle parallax on the art.
    val shift = (pager.currentPage - page) + pager.currentPageOffsetFraction
    val (title, body) = when (page) {
        0 -> s.tourHelloTitle to s.tourHelloBody
        1 -> s.tourThemeTitle to s.tourThemeBody
        2 -> s.tourWriteTitle to s.tourWriteBody
        3 -> s.tourPdfTitle to s.tourPdfBody
        4 -> s.tourGestureTitle to s.tourGestureBody
        else -> s.tourReadyTitle to s.tourReadyBody
    }
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(Radii.lg)).background(c.fill),
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.fillMaxSize().graphicsLayer { translationX = shift * size.width * 0.3f }, contentAlignment = Alignment.Center) {
                when (page) {
                    0 -> HelloArt()
                    1 -> ThemeArt()
                    2 -> WriteArt()
                    3 -> PdfArt()
                    4 -> GestureArt()
                    else -> ReadyArt()
                }
            }
        }
        Spacer(Modifier.height(22.dp))
        Text(title, Modifier.fillMaxWidth(), style = M.type.title.copy(textAlign = TextAlign.Center))
        Spacer(Modifier.height(8.dp))
        Text(body, Modifier.fillMaxWidth(), style = M.type.body.copy(textAlign = TextAlign.Center), color = c.textSecondary)
        Spacer(Modifier.height(20.dp))
        when (page) {
            0 -> LanguagePicker(Modifier.width(380.dp))
            1 -> ThemePicker(Modifier.width(380.dp))
            5 -> Button(s.importPdf, onImportPdf, icon = Lucide.FileUp)
        }
    }
}

@Composable
private fun Dots(pager: PagerState) {
    val c = M.colors
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(PAGES) { i ->
            val active = i == pager.currentPage
            val w by animateDpAsState(if (active) 20.dp else 6.dp, com.mheenotes.ui.design.Motion.bouncy(), label = "dot")
            Box(Modifier.size(w, 6.dp).clip(CircleShape).background(if (active) c.accent else c.fillStrong))
        }
    }
}

// ---- Motion graphics -------------------------------------------------------------------------

private val INK = Color(0xFF1F2A44)
private val PAPER = Color(0xFFFFFFFF)
private val RULE = Color(0xFFE3E7F0)

/** 0..1 repeating every [ms]. */
@Composable
private fun loop(ms: Int): State<Float> =
    rememberInfiniteTransition(label = "loop").animateFloat(0f, 1f, infiniteRepeatable(tween(ms, easing = LinearEasing), RepeatMode.Restart), label = "t")

/** Progress of [t] through [a, b], clamped and eased. */
private fun seg(t: Float, a: Float, b: Float): Float {
    val x = ((t - a) / (b - a)).coerceIn(0f, 1f)
    return FastOutSlowInEasing.transform(x)
}

private fun DrawScope.paper(w: Float, h: Float, lines: Boolean = true): Offset {
    val o = Offset((size.width - w) / 2, (size.height - h) / 2)
    drawRoundRect(Color(0x22000000), o + Offset(0f, 3.dp.toPx()), Size(w, h), CornerRadius(10.dp.toPx()))
    drawRoundRect(PAPER, o, Size(w, h), CornerRadius(10.dp.toPx()))
    if (lines) {
        var y = o.y + 34.dp.toPx()
        while (y < o.y + h - 12.dp.toPx()) {
            drawLine(RULE, Offset(o.x + 14.dp.toPx(), y), Offset(o.x + w - 14.dp.toPx(), y), 1.dp.toPx())
            y += 22.dp.toPx()
        }
    }
    return o
}

private fun DrawScope.partial(path: Path, p: Float, color: Color, width: Float, alpha: Float = 1f): Offset? {
    if (p <= 0f) return null
    val pm = PathMeasure()
    pm.setPath(path, false)
    val out = Path()
    pm.getSegment(0f, pm.length * p, out, true)
    drawPath(out, color.copy(alpha = color.alpha * alpha), style = Stroke(width, cap = StrokeCap.Round, join = StrokeJoin.Round))
    return pm.getPosition(pm.length * p)
}

/** A cursive "hi" written by a pen, then the dot on the i, then it fades and repeats. */
@Composable
private fun HelloArt() {
    val t by loop(3600)
    val accent = M.colors.accent
    Box(contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(320.dp, 190.dp)) {
            val o = paper(size.width * 0.86f, size.height * 0.84f)
            val w = 190.dp.toPx(); val h = 92.dp.toPx()
            val bx = (size.width - w) / 2; val by = o.y + 26.dp.toPx()
            fun p(x: Float, y: Float) = Offset(bx + x * w, by + y * h)
            val path = Path().apply {
                p(0.02f, 0.86f).let { moveTo(it.x, it.y) }
                listOf(
                    listOf(p(0.14f, 0.60f), p(0.30f, 0.02f), p(0.21f, 0.03f)),
                    listOf(p(0.13f, 0.04f), p(0.13f, 0.60f), p(0.15f, 0.96f)),
                    listOf(p(0.19f, 0.60f), p(0.36f, 0.48f), p(0.37f, 0.80f)),
                    listOf(p(0.38f, 0.96f), p(0.46f, 0.98f), p(0.52f, 0.84f)),
                    listOf(p(0.55f, 0.74f), p(0.57f, 0.60f), p(0.58f, 0.54f)),
                    listOf(p(0.57f, 0.76f), p(0.56f, 0.97f), p(0.65f, 0.92f)),
                    listOf(p(0.74f, 0.86f), p(0.86f, 0.72f), p(0.96f, 0.58f)),
                ).forEach { (a, b, e) -> cubicTo(a.x, a.y, b.x, b.y, e.x, e.y) }
            }
            val fade = 1f - seg(t, 0.86f, 0.98f)
            val tip = partial(path, seg(t, 0.04f, 0.62f), INK, 4.5.dp.toPx(), fade)
            val dot = seg(t, 0.64f, 0.72f)
            if (dot > 0f) drawCircle(accent.copy(alpha = fade), 5.dp.toPx() * (1f + 0.4f * sin(dot * PI.toFloat())), p(0.585f, 0.30f))
            // Pen follows the stroke tip; it hops to the dot, then lifts away.
            val pen = when {
                t < 0.62f -> tip
                t < 0.70f -> p(0.585f, 0.30f)
                else -> null
            }
            pen?.let { drawPen(it, fade * (1f - seg(t, 0.70f, 0.80f))) }
        }
    }
}

private fun DrawScope.drawPen(tip: Offset, alpha: Float) {
    if (alpha <= 0f) return
    translate(tip.x, tip.y) {
        rotate(-35f, Offset.Zero) {
            val bw = 9.dp.toPx(); val bl = 62.dp.toPx()
            drawRoundRect(Color(0xFF3A63F0).copy(alpha = alpha), Offset(-bw / 2, -bl - 10.dp.toPx()), Size(bw, bl), CornerRadius(4.dp.toPx()))
            val nib = Path().apply {
                moveTo(-bw / 2, -10.dp.toPx()); lineTo(bw / 2, -10.dp.toPx()); lineTo(0f, 0f); close()
            }
            drawPath(nib, INK.copy(alpha = alpha))
        }
    }
}

/** A tiny library window wiping between light and dark. */
@Composable
private fun ThemeArt() {
    val wipe by rememberInfiniteTransition(label = "theme").animateFloat(
        0.12f, 0.88f, infiniteRepeatable(tween(1800, delayMillis = 500, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "w",
    )
    Canvas(Modifier.size(340.dp, 190.dp)) {
        val w = size.width * 0.9f; val h = size.height * 0.84f
        val o = Offset((size.width - w) / 2, (size.height - h) / 2)
        drawRoundRect(Color(0x22000000), o + Offset(0f, 3.dp.toPx()), Size(w, h), CornerRadius(12.dp.toPx()))
        val split = o.x + w * wipe
        clipRect(right = split) { miniLibrary(o, w, h, dark = false) }
        clipRect(left = split) { miniLibrary(o, w, h, dark = true) }
        drawLine(Color(0xFF3A63F0), Offset(split, o.y), Offset(split, o.y + h), 2.dp.toPx())
        drawCircle(Color.White, 13.dp.toPx(), Offset(split, o.y + h / 2))
        drawCircle(Color(0xFF3A63F0), 13.dp.toPx(), Offset(split, o.y + h / 2), style = Stroke(2.dp.toPx()))
        // Half sun, half moon in the knob.
        val k = Offset(split, o.y + h / 2)
        drawCircle(Color(0xFFE79A2C), 5.dp.toPx(), k)
        drawCircle(Color.White, 4.dp.toPx(), k + Offset(3.dp.toPx(), -2.dp.toPx()))
    }
}

private fun DrawScope.miniLibrary(o: Offset, w: Float, h: Float, dark: Boolean) {
    val bg = if (dark) Color(0xFF0F0F12) else Color(0xFFF4F4F6)
    val side = if (dark) Color(0xFF1B1B1F) else Color(0xFFFFFFFF)
    val line = if (dark) Color(0x33FFFFFF) else Color(0x221C1C24)
    val r = CornerRadius(12.dp.toPx())
    drawRoundRect(bg, o, Size(w, h), r)
    drawRoundRect(side, o, Size(w * 0.26f, h), r)
    drawRect(side, Offset(o.x + w * 0.2f, o.y), Size(w * 0.06f, h))
    repeat(4) { i -> drawRoundRect(line, Offset(o.x + 10.dp.toPx(), o.y + (16 + i * 18).dp.toPx()), Size(w * 0.16f, 6.dp.toPx()), CornerRadius(3.dp.toPx())) }
    drawRoundRect(Color(0xFF3A63F0).copy(alpha = 0.85f), Offset(o.x + 10.dp.toPx(), o.y + 16.dp.toPx()), Size(w * 0.16f, 6.dp.toPx()), CornerRadius(3.dp.toPx()))
    val cw = w * 0.14f; val ch = h * 0.5f
    val covers = listOf(Color(0xFFFFFFFF), Color(0xFFFFF4D6), Color(0xFFE4ECFF), Color(0xFFFFFFFF))
    repeat(4) { i ->
        val x = o.x + w * 0.31f + i * (cw + w * 0.025f)
        val y = o.y + h * 0.28f
        drawRoundRect(covers[i], Offset(x, y), Size(cw, ch), CornerRadius(6.dp.toPx()))
        repeat(3) { j -> drawLine(Color(0x553A63F0), Offset(x + 6.dp.toPx(), y + (14 + j * 10).dp.toPx()), Offset(x + cw - 8.dp.toPx(), y + (14 + j * 10).dp.toPx()), 1.5.dp.toPx()) }
        drawRoundRect(line, Offset(x, y + ch + 8.dp.toPx()), Size(cw * 0.7f, 5.dp.toPx()), CornerRadius(3.dp.toPx()))
    }
    drawRoundRect(line, Offset(o.x + w * 0.31f, o.y + 12.dp.toPx()), Size(w * 0.25f, 9.dp.toPx()), CornerRadius(4.dp.toPx()))
}

/** Write a wave, snap a wobbly circle into a clean one, then scribble the wave away. */
@Composable
private fun WriteArt() {
    val t by loop(6400)
    val accent = M.colors.accent
    val danger = M.colors.danger
    Canvas(Modifier.size(340.dp, 190.dp)) {
        val o = paper(size.width * 0.9f, size.height * 0.86f)
        val pw = size.width * 0.9f
        val stroke = 3.5.dp.toPx()
        val fadeAll = 1f - seg(t, 0.92f, 0.99f)
        // 1. A wave of handwriting.
        val wave = Path().apply {
            val x0 = o.x + 26.dp.toPx(); val y0 = o.y + 52.dp.toPx(); val len = pw * 0.5f
            moveTo(x0, y0)
            var i = 1
            while (i <= 40) { val f = i / 40f; lineTo(x0 + len * f, y0 + sin(f * 4.5f * PI.toFloat()) * 9.dp.toPx() - f * 4.dp.toPx()); i++ }
        }
        val erased = seg(t, 0.80f, 0.88f)
        partial(wave, seg(t, 0.02f, 0.22f), INK, stroke, (1f - erased) * fadeAll)?.let { if (t < 0.22f) drawPen(it, 1f) }
        // 2. A rough circle that snaps clean after a hold.
        val cx = o.x + pw * 0.76f; val cy = o.y + size.height * 0.42f; val r = 30.dp.toPx()
        val rough = Path().apply {
            var i = 0
            while (i <= 48) {
                val a = i / 48f * 2.1f * PI.toFloat() - PI.toFloat() / 2
                val rr = r * (1f + 0.09f * sin(a * 3f) + 0.05f * cos(a * 5f))
                val x = cx + cos(a) * rr; val y = cy + sin(a) * rr
                if (i == 0) moveTo(x, y) else lineTo(x, y)
                i++
            }
        }
        val snap = seg(t, 0.46f, 0.54f)
        partial(rough, seg(t, 0.26f, 0.42f), INK, stroke, (1f - snap) * fadeAll)?.let { if (t < 0.42f) drawPen(it, 1f) }
        if (t in 0.42f..0.48f) drawCircle(accent.copy(alpha = 0.25f * sin(seg(t, 0.42f, 0.48f) * PI.toFloat())), r * 1.5f, Offset(cx, cy))
        if (snap > 0f) scale(0.9f + 0.1f * snap, Offset(cx, cy)) { drawCircle(accent.copy(alpha = snap * fadeAll), r, Offset(cx, cy), style = Stroke(stroke)) }
        // 3. Scribble over the wave to erase it.
        val scribble = Path().apply {
            val x0 = o.x + 30.dp.toPx(); val y0 = o.y + 50.dp.toPx(); val len = pw * 0.46f
            moveTo(x0, y0)
            var i = 1
            while (i <= 14) { val f = i / 14f; lineTo(x0 + len * f, y0 + (if (i % 2 == 0) -12 else 12).dp.toPx()); i++ }
        }
        partial(scribble, seg(t, 0.60f, 0.78f), danger, stroke * 0.9f, 1f - erased)?.let { if (t < 0.78f) drawPen(it, 1f) }
        if (erased in 0.01f..0.99f) repeat(6) { k ->
            val a = k / 6f * 2 * PI.toFloat()
            val d = 20.dp.toPx() + erased * 26.dp.toPx()
            drawCircle(accent.copy(alpha = 1f - erased), 3.dp.toPx(), Offset(o.x + pw * 0.28f + cos(a) * d, o.y + 50.dp.toPx() + sin(a) * d * 0.6f))
        }
    }
}

/** A PDF hops along an arc into a notebook, which bounces as it lands. */
@Composable
private fun PdfArt() {
    val t by loop(3000)
    val danger = M.colors.danger
    Canvas(Modifier.size(340.dp, 190.dp)) {
        val fly = seg(t, 0.15f, 0.62f)
        val land = seg(t, 0.58f, 0.80f)
        // Notebook on the right.
        val nb = Offset(size.width * 0.70f, size.height * 0.52f)
        val bounce = 1f + 0.12f * sin(land * PI.toFloat())
        scale(bounce, nb) {
            val nw = 84.dp.toPx(); val nh = 110.dp.toPx()
            for (k in 2 downTo 0) {
                val off = k * 5.dp.toPx()
                drawRoundRect(if (k == 0) PAPER else Color(0xFFE4E8F2), Offset(nb.x - nw / 2 + off, nb.y - nh / 2 - off), Size(nw, nh), CornerRadius(8.dp.toPx()))
            }
            drawRoundRect(Color(0xFF3A63F0), Offset(nb.x - nw / 2, nb.y - nh / 2), Size(8.dp.toPx(), nh), CornerRadius(4.dp.toPx()))
            repeat(4) { j ->
                val y = nb.y - nh / 2 + (22 + j * 18).dp.toPx()
                drawLine(RULE, Offset(nb.x - nw / 2 + 16.dp.toPx(), y), Offset(nb.x + nw / 2 - 10.dp.toPx(), y), 2.dp.toPx())
            }
            if (land > 0f) drawCircle(Color(0xFF2FA36B).copy(alpha = land), 11.dp.toPx(), Offset(nb.x + nw / 2 - 4.dp.toPx(), nb.y - nh / 2 + 4.dp.toPx()))
        }
        // The PDF document flying in.
        val start = Offset(size.width * 0.24f, size.height * 0.52f)
        val x = start.x + (nb.x - start.x) * fly
        val y = start.y - sin(fly * PI.toFloat()) * 60.dp.toPx() + (nb.y - start.y) * fly
        val a = 1f - seg(t, 0.56f, 0.64f)
        if (a > 0f) {
            val sc = 1f - 0.4f * fly
            translate(x, y) {
                rotate(-12f * sin(fly * PI.toFloat()), Offset.Zero) {
                    scale(sc, Offset.Zero) {
                        val dw = 70.dp.toPx(); val dh = 92.dp.toPx()
                        drawRoundRect(Color(0x22000000).copy(alpha = 0.13f * a), Offset(-dw / 2, -dh / 2 + 3.dp.toPx()), Size(dw, dh), CornerRadius(6.dp.toPx()))
                        drawRoundRect(PAPER.copy(alpha = a), Offset(-dw / 2, -dh / 2), Size(dw, dh), CornerRadius(6.dp.toPx()))
                        drawRoundRect(danger.copy(alpha = a), Offset(-dw / 2 + 8.dp.toPx(), -dh / 2 + 10.dp.toPx()), Size(30.dp.toPx(), 14.dp.toPx()), CornerRadius(3.dp.toPx()))
                        repeat(4) { j ->
                            val ly = -dh / 2 + (36 + j * 12).dp.toPx()
                            drawLine(Color(0xFFCBD2E0).copy(alpha = a), Offset(-dw / 2 + 8.dp.toPx(), ly), Offset(dw / 2 - (8 + (j % 2) * 12).dp.toPx(), ly), 3.dp.toPx(), cap = StrokeCap.Round)
                        }
                    }
                }
            }
        }
        // Dotted trail.
        repeat(7) { k ->
            val f = k / 7f
            if (f < fly) {
                val tx = start.x + (nb.x - start.x) * f
                val ty = start.y - sin(f * PI.toFloat()) * 60.dp.toPx() + (nb.y - start.y) * f
                drawCircle(Color(0xFF3A63F0).copy(alpha = 0.35f * (1f - land)), 2.5.dp.toPx(), Offset(tx, ty))
            }
        }
    }
}

/** Two fingers tap (undo), three fingers tap (redo), then a pinch zooms the page. */
@Composable
private fun GestureArt() {
    val t by loop(6000)
    val accent = M.colors.accent
    Box(contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(340.dp, 190.dp)) {
            val o = paper(size.width * 0.78f, size.height * 0.86f)
            val c = Offset(size.width / 2, size.height / 2)
            val zoom = 1f + 0.45f * seg(t, 0.72f, 0.88f) * (1f - seg(t, 0.94f, 1f))
            scale(zoom, c) {
                val wave = Path().apply {
                    val x0 = o.x + 40.dp.toPx(); val y0 = o.y + 60.dp.toPx()
                    moveTo(x0, y0)
                    var i = 1
                    while (i <= 30) { val f = i / 30f; lineTo(x0 + 140.dp.toPx() * f, y0 + sin(f * 3.5f * PI.toFloat()) * 8.dp.toPx()); i++ }
                }
                drawPath(wave, INK, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
                drawLine(INK, Offset(o.x + 40.dp.toPx(), o.y + 96.dp.toPx()), Offset(o.x + 150.dp.toPx(), o.y + 96.dp.toPx()), 3.dp.toPx(), cap = StrokeCap.Round)
            }
            fun finger(p: Offset, press: Float) {
                drawCircle(accent.copy(alpha = 0.18f), 18.dp.toPx() * (1f + 0.5f * press), p)
                drawCircle(accent.copy(alpha = 0.55f), 11.dp.toPx() * (1f - 0.15f * press), p)
            }
            fun tap(a: Float, b: Float) = sin(seg(t, a, b) * PI.toFloat())
            val y = size.height * 0.64f
            if (t < 0.30f) {
                val p = tap(0.06f, 0.20f)
                listOf(-18f, 18f).forEach { dx -> finger(Offset(c.x + dx.dp.toPx(), y), p) }
            } else if (t < 0.62f) {
                val p = tap(0.38f, 0.52f)
                listOf(-30f, 0f, 30f).forEach { dx -> finger(Offset(c.x + dx.dp.toPx(), y - (if (dx == 0f) 8 else 0).dp.toPx()), p) }
            } else if (t < 0.92f) {
                val spread = seg(t, 0.72f, 0.88f)
                val d = (16 + 46 * spread).dp.toPx()
                finger(Offset(c.x - d, y + d * 0.2f), 0f)
                finger(Offset(c.x + d, y - d * 0.6f), 0f)
            }
        }
        // Undo / redo badges pop above the fingers.
        val undo = sin(seg(t, 0.12f, 0.30f) * PI.toFloat())
        val redo = sin(seg(t, 0.44f, 0.62f) * PI.toFloat())
        Badge(Lucide.Undo2, undo)
        Badge(Lucide.Redo2, redo)
    }
}

@Composable
private fun BoxScope.Badge(icon: androidx.compose.ui.graphics.vector.ImageVector, k: Float) {
    if (k <= 0.01f) return
    Box(
        Modifier.align(Alignment.Center).graphicsLayer { translationY = -36.dp.toPx() - 10.dp.toPx() * k; scaleX = 0.6f + 0.4f * k; scaleY = scaleX; alpha = k }
            .size(44.dp).clip(CircleShape).background(M.colors.accent),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, tint = M.colors.onAccent, size = 22.dp) }
}

/** Confetti bursting from an open notebook. */
@Composable
private fun ReadyArt() {
    val t by loop(2800)
    val colors = listOf(Color(0xFF3A63F0), Color(0xFF2FA36B), Color(0xFFE79A2C), Color(0xFFE5484D), Color(0xFFB45CF0), Color(0xFF22B8CF))
    val pieces = remember {
        val rnd = kotlin.random.Random(7)
        List(42) { Triple(rnd.nextFloat() * 2 * PI.toFloat(), 0.5f + rnd.nextFloat() * 0.8f, rnd.nextFloat()) }
    }
    Box(contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(340.dp, 190.dp)) {
            val c = Offset(size.width / 2, size.height * 0.62f)
            val burst = seg(t, 0f, 0.55f)
            val fall = ((t - 0.2f) / 0.8f).coerceIn(0f, 1f)
            pieces.forEachIndexed { i, (a, speed, spin) ->
                val d = burst * 120.dp.toPx() * speed
                val x = c.x + cos(a) * d
                val y = c.y - abs(sin(a)) * d * 1.1f + fall * fall * 90.dp.toPx()
                val alpha = 1f - seg(t, 0.75f, 1f)
                translate(x, y) {
                    rotate(spin * 360f + t * 540f * (if (i % 2 == 0) 1 else -1), Offset.Zero) {
                        drawRect(colors[i % colors.size].copy(alpha = alpha), Offset(-4.dp.toPx(), -2.dp.toPx()), Size(8.dp.toPx(), 4.dp.toPx()))
                    }
                }
            }
            // Open notebook.
            val pop = 1f + 0.08f * sin(seg(t, 0f, 0.25f) * PI.toFloat())
            scale(pop, c) {
                val w = 70.dp.toPx(); val h = 50.dp.toPx()
                drawRoundRect(Color(0x22000000), Offset(c.x - w, c.y - h / 2 + 3.dp.toPx()), Size(w * 2, h), CornerRadius(6.dp.toPx()))
                drawRoundRect(PAPER, Offset(c.x - w, c.y - h / 2), Size(w - 2.dp.toPx(), h), CornerRadius(6.dp.toPx()))
                drawRoundRect(PAPER, Offset(c.x + 2.dp.toPx(), c.y - h / 2), Size(w - 2.dp.toPx(), h), CornerRadius(6.dp.toPx()))
                repeat(3) { j ->
                    val ly = c.y - h / 2 + (14 + j * 11).dp.toPx()
                    drawLine(RULE, Offset(c.x - w + 10.dp.toPx(), ly), Offset(c.x - 10.dp.toPx(), ly), 2.dp.toPx())
                    drawLine(RULE, Offset(c.x + 10.dp.toPx(), ly), Offset(c.x + w - 10.dp.toPx(), ly), 2.dp.toPx())
                }
                drawLine(Color(0xFF3A63F0), Offset(c.x, c.y - h / 2), Offset(c.x, c.y + h / 2), 2.dp.toPx())
            }
        }
        Icon(Lucide.PenLine, Modifier.graphicsLayer { translationX = 40.dp.toPx(); translationY = 4.dp.toPx() }, tint = M.colors.accent, size = 26.dp)
    }
}
