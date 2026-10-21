package com.mheenotes.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.remember
import com.mheenotes.ui.App
import com.mheenotes.ui.AppGraph
import com.mheenotes.ui.AppState
import kotlinx.coroutines.launch

/** An extra window ("Open in new window") with its own tabs, next to the main one. */
class WindowActivity : ComponentActivity() {
    private val state = AppState()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        AppState.windows.add(state)
        intent.getStringExtra("openId")?.let { id ->
            AppGraph.scope.launch {
                AppGraph.awaitLoaded()
                if (!state.open(id)) finish()
            }
        }
        setContent { App(remember { state }) }
    }

    override fun onStop() {
        super.onStop()
        state.tabs.forEach { it.onLeave() }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) {
            state.tabs.toList().forEach { state.closeTab(it.notebookId) }
            AppState.windows.remove(state)
        }
    }
}
