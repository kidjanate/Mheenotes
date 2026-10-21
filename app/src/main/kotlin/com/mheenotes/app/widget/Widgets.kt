package com.mheenotes.app.widget

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.mheenotes.app.MainActivity
import com.mheenotes.core.model.Notebook
import com.mheenotes.ui.AppGraph
import com.mheenotes.ui.i18n.strings
import com.mheenotes.ui.library.relativeTime
import java.io.File

private val Bg = ColorProvider(day = Color(0xFFFFFFFF), night = Color(0xFF1B1B1F))
private val Fg = ColorProvider(day = Color(0xFF1C1C1F), night = Color(0xFFF4F4F6))
private val Muted = ColorProvider(day = Color(0xFF6B6B72), night = Color(0xFFA1A1A8))
private val Fill = ColorProvider(day = Color(0xFFF4F4F6), night = Color(0xFF26262B))


private fun openIntent(context: Context, notebookId: String?) =
    Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        notebookId?.let { putExtra("openId", it) }
    }

/** Three most recently opened notebooks with their covers. */
class RecentNotesWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        AppGraph.awaitLoaded()
        val recent = AppGraph.library.state.value.notebooks.filter { it.trashedAt == null }.sortedByDescending { it.openedAt }.take(3)
        val covers = recent.associate { n -> n.id to cover(n) }
        provideContent { Recent(context, recent, covers) }
    }

    private fun cover(n: Notebook): Bitmap? {
        val f = File(AppGraph.files.thumb(n.id))
        if (!f.exists()) return null
        return BitmapFactory.decodeFile(f.path, BitmapFactory.Options().apply { inSampleSize = 2 })
    }

    @Composable
    private fun Recent(context: Context, recent: List<Notebook>, covers: Map<String, Bitmap?>) {
        val s = strings()
        Column(GlanceModifier.fillMaxSize().background(Bg).cornerRadius(24.dp).padding(14.dp)) {
            Text(s.recents, style = TextStyle(color = Muted, fontSize = 12.sp, fontWeight = FontWeight.Medium))
            Spacer(GlanceModifier.height(10.dp))
            if (recent.isEmpty()) {
                Text(s.noNotesTitle, style = TextStyle(color = Fg, fontSize = 14.sp))
            }
            Row(GlanceModifier.fillMaxWidth()) {
                recent.forEachIndexed { i, n ->
                    if (i > 0) Spacer(GlanceModifier.width(10.dp))
                    Column(GlanceModifier.defaultWeight().clickable(actionStartActivity(openIntent(context, n.id)))) {
                        Box(GlanceModifier.fillMaxWidth().height(96.dp).cornerRadius(10.dp).background(Fill)) {
                            covers[n.id]?.let { Image(ImageProvider(it), n.title, GlanceModifier.fillMaxSize(), contentScale = ContentScale.Crop) }
                        }
                        Spacer(GlanceModifier.height(6.dp))
                        Text(n.title, maxLines = 1, style = TextStyle(color = Fg, fontSize = 13.sp, fontWeight = FontWeight.Medium))
                        Text(relativeTime(n.openedAt, s), maxLines = 1, style = TextStyle(color = Muted, fontSize = 11.sp))
                    }
                }
            }
        }
    }
}

/** Strokes written per day over the last week, as a bar graph. */
class InkActivityWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        AppGraph.awaitLoaded()
        val days = AppGraph.library.activity(7)
        val night = context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK == android.content.res.Configuration.UI_MODE_NIGHT_YES
        val graph = graph(days, night)
        provideContent {
            val s = strings()
            Column(
                GlanceModifier.fillMaxSize().background(Bg).cornerRadius(24.dp).padding(14.dp)
                    .clickable(actionStartActivity(openIntent(context, null))),
            ) {
                Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(s.inkActivity, GlanceModifier.defaultWeight(), style = TextStyle(color = Muted, fontSize = 12.sp, fontWeight = FontWeight.Medium))
                    Text(days.sum().toString(), style = TextStyle(color = Fg, fontSize = 16.sp, fontWeight = FontWeight.Bold))
                }
                Spacer(GlanceModifier.height(8.dp))
                Image(ImageProvider(graph), s.inkActivity, GlanceModifier.fillMaxWidth().defaultWeight(), contentScale = ContentScale.FillBounds)
            }
        }
    }

    private fun graph(days: List<Int>, night: Boolean): Bitmap {
        val w = 700; val h = 220
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val max = (days.maxOrNull() ?: 0).coerceAtLeast(1)
        val gap = 18f
        val barW = (w - gap * (days.size - 1)) / days.size
        val accent = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = if (night) 0xFF7090FF.toInt() else 0xFF3A63F0.toInt() }
        val track = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = if (night) 0xFF26262B.toInt() else 0xFFEDEEF2.toInt() }
        days.forEachIndexed { i, v ->
            val x = i * (barW + gap)
            c.drawRoundRect(RectF(x, 0f, x + barW, h.toFloat()), 14f, 14f, track)
            val bh = (h * v / max.toFloat()).coerceAtLeast(if (v > 0) 16f else 0f)
            if (bh > 0) c.drawRoundRect(RectF(x, h - bh, x + barW, h.toFloat()), 14f, 14f, accent)
        }
        return bmp
    }
}

class RecentNotesReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = RecentNotesWidget()
}

class InkActivityReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = InkActivityWidget()
}

/** Refreshes every placed widget; cheap no-op when none are on the home screen. */
suspend fun refreshWidgets(context: Context) {
    val m = GlanceAppWidgetManager(context)
    if (m.getGlanceIds(RecentNotesWidget::class.java).isNotEmpty()) RecentNotesWidget().updateAll(context)
    if (m.getGlanceIds(InkActivityWidget::class.java).isNotEmpty()) InkActivityWidget().updateAll(context)
}

