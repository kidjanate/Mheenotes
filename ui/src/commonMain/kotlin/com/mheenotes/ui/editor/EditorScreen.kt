package com.mheenotes.ui.editor

import androidx.compose.runtime.Composable
import com.mheenotes.ui.AppState
import com.mheenotes.ui.design.Button

@Composable
fun EditorScreen(app: AppState) {
    Button("Back", { app.backToLibrary() })
}
