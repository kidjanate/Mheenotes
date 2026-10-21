package com.mheenotes.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.mheenotes.ui.App
import com.mheenotes.ui.AppGraph
import com.mheenotes.ui.AppState
import com.mheenotes.ui.dev.Fixtures
import com.mheenotes.ui.platform.LegacyImport
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        handle(intent)
        setContent { App() }
    }

    override fun onStop() {
        super.onStop()
        // Covers, the library index and widgets catch up whenever the app leaves the screen.
        AppState.shared.tabs.forEach { it.onLeave() }
        AppGraph.scope.launch {
            kotlinx.coroutines.delay(2500) // let cover rendering finish first
            com.mheenotes.app.widget.refreshWidgets(applicationContext)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handle(intent)
    }

    private fun handle(intent: Intent?) {
        intent ?: return
        // Debug-only hooks for performance runs: `am start --es fixture library|dense`.
        if (Fixtures.enabled) when (intent.getStringExtra("fixture")) {
            "library" -> Fixtures.library(intent.getIntExtra("count", 1000))
            "dense" -> Fixtures.densePage(intent.getIntExtra("count", 20_000))
        }
        if (Fixtures.enabled) intent.getStringExtra("importLegacy")?.let { path ->
            AppGraph.scope.launch {
                runCatching { LegacyImport.importFile(java.io.File(filesDir, path), null) }
                    .onSuccess { AppGraph.library.upsert(it) }.onFailure { it.printStackTrace() }
            }
        }
        intent.getStringExtra("openId")?.let { id ->
            AppGraph.scope.launch { AppGraph.awaitLoaded(); if (AppGraph.library.notebook(id) != null) AppState.shared.open(id) }
        }
        intent.getStringExtra("open")?.let { title ->
            AppGraph.library.state.value.notebooks.filter { it.title == title }.maxByOrNull { it.openedAt }?.let { AppState.shared.open(it.id) }
        }
        intent.removeExtra("fixture")
    }
}
