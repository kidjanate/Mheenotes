package com.mheenotes.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.mheenotes.app.MainActivity
import com.mheenotes.app.R
import com.mheenotes.app.ui.AppSettingsStore
import com.mheenotes.core.storage.NativeNotebookContainer

class RecentNotesWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { manager.updateAppWidget(it, views(context)) }
    }

    companion object {
        private val rowIds = intArrayOf(R.id.recent_note_1, R.id.recent_note_2, R.id.recent_note_3)

        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, RecentNotesWidgetProvider::class.java)
            manager.getAppWidgetIds(component).forEach { manager.updateAppWidget(it, views(context)) }
        }

        private fun views(context: Context): RemoteViews {
            val theme = AppSettingsStore(context).load().activeTheme
            val notes = context.filesDir.resolve("notebooks").listFiles().orEmpty()
                .mapNotNull { directory ->
                    runCatching { NativeNotebookContainer.loadManifest(directory) }.getOrNull()
                        ?.takeIf { it.trashedAtEpochMillis == null }
                        ?.let { directory.name to it }
                }
                .sortedByDescending { it.second.lastOpenedAtEpochMillis }
                .take(3)
            return RemoteViews(context.packageName, R.layout.widget_recent_notes).apply {
                setInt(R.id.widget_root, "setBackgroundColor", theme.surfaceArgb)
                setTextColor(R.id.widget_title, theme.primaryArgb)
                rowIds.forEachIndexed { index, rowId ->
                    val note = notes.getOrNull(index)
                    setViewVisibility(rowId, if (note == null) View.GONE else View.VISIBLE)
                    if (note != null) {
                        setTextViewText(rowId, note.second.title)
                        setTextColor(rowId, if (theme.dark) 0xFFF1F3F7.toInt() else 0xFF171A20.toInt())
                        val intent = Intent(context, MainActivity::class.java).apply {
                            putExtra(MainActivity.EXTRA_NOTEBOOK_ID, note.second.id.value)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
                        }
                        setOnClickPendingIntent(
                            rowId,
                            PendingIntent.getActivity(
                                context,
                                note.second.id.value.hashCode(),
                                intent,
                                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                            ),
                        )
                    }
                }
                setTextViewText(R.id.widget_empty, if (notes.isEmpty()) context.getString(R.string.widget_no_notes) else "")
                setTextColor(R.id.widget_empty, if (theme.dark) 0xFFBBC2CD.toInt() else 0xFF535A64.toInt())
            }
        }
    }
}
