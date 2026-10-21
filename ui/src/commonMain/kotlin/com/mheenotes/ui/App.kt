package com.mheenotes.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.mheenotes.core.model.Id
import com.mheenotes.core.model.nowMillis
import com.mheenotes.ui.design.M
import com.mheenotes.ui.design.MheenotesTheme
import com.mheenotes.ui.design.Motion
import com.mheenotes.ui.design.OverlayHost
import com.mheenotes.ui.editor.EditorScreen
import com.mheenotes.ui.editor.EditorTab
import com.mheenotes.ui.library.LibraryScreen

enum class Screen { Library, Editor }

/** Navigation and open tabs. Lives for the whole process so tabs survive config changes. */
class AppState {
    var screen by mutableStateOf(Screen.Library)
    val tabs = mutableStateListOf<EditorTab>()
    var activeTabId by mutableStateOf<Id?>(null)

    val activeTab: EditorTab? get() = tabs.firstOrNull { it.notebookId == activeTabId }

    fun open(notebookId: Id) {
        AppGraph.library.update(notebookId) { it.copy(openedAt = nowMillis()) }
        if (tabs.none { it.notebookId == notebookId }) tabs += EditorTab(notebookId)
        activeTabId = notebookId
        screen = Screen.Editor
    }

    fun closeTab(notebookId: Id) {
        val i = tabs.indexOfFirst { it.notebookId == notebookId }
        if (i < 0) return
        tabs.removeAt(i).close()
        if (activeTabId == notebookId) {
            activeTabId = tabs.getOrNull((i - 1).coerceAtLeast(0))?.notebookId
            if (activeTabId == null) screen = Screen.Library
        }
    }

    fun backToLibrary() {
        tabs.forEach { it.onLeave() }
        screen = Screen.Library
    }

    companion object {
        val shared by lazy { AppState() }
    }
}

@Composable
fun App(state: AppState = remember { AppState.shared }) {
    MheenotesTheme(AppGraph.settings.theme) {
        SystemBars(M.colors.isDark)
        OverlayHost {
            Box(Modifier.fillMaxSize().background(M.colors.background)) {
                val lib by AppGraph.library.state.collectAsState()
                if (lib.loaded) {
                    AnimatedContent(
                        state.screen,
                        transitionSpec = {
                            if (targetState == Screen.Editor) {
                                (scaleIn(Motion.standard(), initialScale = 0.96f) + fadeIn(Motion.fade()))
                                    .togetherWith(scaleOut(Motion.standard(), targetScale = 1.02f) + fadeOut(Motion.fade()))
                            } else {
                                (scaleIn(Motion.standard(), initialScale = 1.02f) + fadeIn(Motion.fade()))
                                    .togetherWith(scaleOut(Motion.standard(), targetScale = 0.97f) + fadeOut(Motion.fade()))
                            }
                        },
                        label = "screen",
                    ) { screen ->
                        when (screen) {
                            Screen.Library -> LibraryScreen(state)
                            Screen.Editor -> EditorScreen(state)
                        }
                    }
                }
            }
        }
    }
}

/** Keeps status/navigation bar icons readable for the app theme (not just the system one). */
@Composable
expect fun SystemBars(dark: Boolean)
