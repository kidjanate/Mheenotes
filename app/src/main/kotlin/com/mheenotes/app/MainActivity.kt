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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        handle(intent)
        setContent { App() }
    }

    override fun onStop() {
        super.onStop()
        // Covers and the library index catch up whenever the app leaves the screen.
        AppState.shared.tabs.forEach { it.onLeave() }
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
        intent.getStringExtra("open")?.let { title ->
            AppGraph.library.state.value.notebooks.firstOrNull { it.title == title }?.let { AppState.shared.open(it.id) }
        }
        intent.removeExtra("fixture")
    }
}
