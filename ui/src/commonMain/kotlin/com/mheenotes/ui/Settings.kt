package com.mheenotes.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.mheenotes.core.store.Library
import com.mheenotes.ui.design.ThemeMode

/** UI language: follow the device, or a fixed choice inside the app. */
enum class AppLanguage { System, English, Thai }

/** Makes [language] the process locale, so string resources (and formatting) resolve in it. */
expect fun applyAppLanguage(language: AppLanguage)

/** Provides [language] to everything below, so every string resource re-resolves when it changes. */
@androidx.compose.runtime.Composable
expect fun ProvideAppLanguage(language: AppLanguage, content: @androidx.compose.runtime.Composable () -> Unit)

enum class LibrarySort { Edited, Opened, Title, Created, Pages }

/** User preferences, persisted as key/value rows in the library database. */
class Settings {
    private var library: Library? = null

    var theme by mutableStateOf(ThemeMode.System)
        private set
    var librarySort by mutableStateOf(LibrarySort.Edited)
        private set
    var libraryGrid by mutableStateOf(true)
        private set
    var fingerDraws by mutableStateOf(false)
        private set
    var scribbleErase by mutableStateOf(true)
        private set
    var holdToShape by mutableStateOf(true)
        private set
    var language by mutableStateOf(AppLanguage.System)
        private set
    var askInsertPage by mutableStateOf(true)
        private set
    var pressure by mutableStateOf(true)
        private set
    var lowLatencyInk by mutableStateOf(true)
        private set
    var toolbarDock by mutableStateOf("Top")
        private set
    /** False until the welcome tour has been finished or skipped once. */
    var onboarded by mutableStateOf(true)
        private set
    /** Serialized tool presets, owned by the editor. */
    var toolPresets by mutableStateOf("")
        private set

    suspend fun load(lib: Library) {
        library = lib
        lib.setting("theme")?.let { v -> ThemeMode.entries.firstOrNull { it.name == v }?.let { theme = it } }
        lib.setting("sort")?.let { v -> LibrarySort.entries.firstOrNull { it.name == v }?.let { librarySort = it } }
        lib.setting("grid")?.let { libraryGrid = it == "1" }
        lib.setting("finger")?.let { fingerDraws = it == "1" }
        lib.setting("scribble")?.let { scribbleErase = it == "1" }
        lib.setting("holdShape")?.let { holdToShape = it == "1" }
        lib.setting("tools")?.let { toolPresets = it }
        lib.setting("dock")?.let { toolbarDock = it }
        lib.setting("lowLatency")?.let { lowLatencyInk = it == "1" }
        lib.setting("askInsert")?.let { askInsertPage = it == "1" }
        lib.setting("language")?.let { v -> AppLanguage.entries.firstOrNull { it.name == v }?.let { language = it } }
        lib.setting("pressure")?.let { pressure = it == "1" }
        onboarded = lib.setting("onboarded") == "1"
        applyAppLanguage(language)
    }

    private fun put(k: String, v: String) = library?.putSetting(k, v)
    private fun Boolean.s() = if (this) "1" else "0"

    fun updateTheme(v: ThemeMode) { theme = v; put("theme", v.name) }
    fun updateSort(v: LibrarySort) { librarySort = v; put("sort", v.name) }
    fun updateGrid(v: Boolean) { libraryGrid = v; put("grid", v.s()) }
    fun updateFingerDraws(v: Boolean) { fingerDraws = v; put("finger", v.s()) }
    fun updateScribble(v: Boolean) { scribbleErase = v; put("scribble", v.s()) }
    fun updateHoldShape(v: Boolean) { holdToShape = v; put("holdShape", v.s()) }
    fun updateTools(v: String) { toolPresets = v; put("tools", v) }
    fun updateDock(v: String) { toolbarDock = v; put("dock", v) }
    fun updateLowLatency(v: Boolean) { lowLatencyInk = v; put("lowLatency", v.s()) }
    fun updateAskInsert(v: Boolean) { askInsertPage = v; put("askInsert", v.s()) }
    fun updateLanguage(v: AppLanguage) { language = v; applyAppLanguage(v); put("language", v.name) }
    fun updatePressure(v: Boolean) { pressure = v; put("pressure", v.s()) }
    fun finishOnboarding() { onboarded = true; put("onboarded", "1") }
}
