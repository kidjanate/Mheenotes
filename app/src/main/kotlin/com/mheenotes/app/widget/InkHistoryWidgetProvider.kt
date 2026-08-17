package com.mheenotes.app.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.widget.RemoteViews
import com.mheenotes.app.R
import com.mheenotes.app.ui.AppSettingsStore

class InkHistoryWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { manager.updateAppWidget(it, views(context)) }
    }

    companion object {
        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, InkHistoryWidgetProvider::class.java)
            manager.getAppWidgetIds(component).forEach { manager.updateAppWidget(it, views(context)) }
        }

        private fun views(context: Context): RemoteViews {
            val theme = AppSettingsStore(context).load().activeTheme
            val values = InkActivityTracker.lastSevenDays(context)
            return RemoteViews(context.packageName, R.layout.widget_ink_history).apply {
                setInt(R.id.widget_root, "setBackgroundColor", theme.surfaceArgb)
                setTextColor(R.id.widget_title, theme.primaryArgb)
                setTextColor(R.id.ink_total, if (theme.dark) 0xFFF1F3F7.toInt() else 0xFF171A20.toInt())
                setTextViewText(R.id.ink_total, context.getString(R.string.widget_stroke_count, values.sumOf { it.second }))
                setImageViewBitmap(R.id.ink_graph, graph(values, theme.primaryArgb, theme.dark))
            }
        }

        private fun graph(values: List<Pair<String, Int>>, color: Int, dark: Boolean): Bitmap {
            val bitmap = Bitmap.createBitmap(600, 240, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val line = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color; strokeWidth = 10f; style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND }
            val dot = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color; style = Paint.Style.FILL }
            val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = if (dark) 0xFFBBC2CD.toInt() else 0xFF535A64.toInt(); textSize = 26f; textAlign = Paint.Align.CENTER }
            val maximum = values.maxOfOrNull { it.second }?.coerceAtLeast(1) ?: 1
            val points = values.mapIndexed { index, (_, count) ->
                val x = 34f + index * (532f / 6f)
                val y = 178f - count.toFloat() / maximum * 136f
                x to y
            }
            val path = android.graphics.Path()
            points.forEachIndexed { index, (x, y) -> if (index == 0) path.moveTo(x, y) else path.lineTo(x, y) }
            canvas.drawPath(path, line)
            points.forEach { (x, y) -> canvas.drawCircle(x, y, 11f, dot) }
            values.forEachIndexed { index, value -> canvas.drawText(value.first, points[index].first, 226f, text) }
            return bitmap
        }
    }
}
