package com.mheenotes.app.widget

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object InkActivityTracker {
    private const val PreferencesName = "ink-activity"

    fun recordStroke(context: Context) {
        val key = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val preferences = context.getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)
        preferences.edit().putInt(key, preferences.getInt(key, 0) + 1).apply()
        InkHistoryWidgetProvider.updateAll(context)
    }

    fun lastSevenDays(context: Context): List<Pair<String, Int>> {
        val preferences = context.getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val label = SimpleDateFormat("EEE", Locale.getDefault())
        val calendar = java.util.Calendar.getInstance()
        calendar.add(java.util.Calendar.DAY_OF_YEAR, -6)
        return List(7) {
            val date = calendar.time
            (label.format(date) to preferences.getInt(format.format(date), 0)).also {
                calendar.add(java.util.Calendar.DAY_OF_YEAR, 1)
            }
        }
    }
}
