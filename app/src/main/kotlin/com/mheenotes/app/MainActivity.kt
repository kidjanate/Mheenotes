package com.mheenotes.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Build
import android.view.KeyEvent
import android.view.InputDevice
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.mheenotes.app.ui.MheenotesTheme
import com.mheenotes.app.ui.AppSettingsStore
import com.mheenotes.app.ui.editor.MheenotesEditor
import com.mheenotes.app.widget.InkHistoryWidgetProvider
import com.mheenotes.app.widget.RecentNotesWidgetProvider

class MainActivity : ComponentActivity() {
    private val incomingUri = mutableStateOf<Uri?>(null)
    private val incomingNotebookId = mutableStateOf<String?>(null)
    private val hardwareKeySequence = mutableIntStateOf(0)
    private val hardwareKeyCode = mutableIntStateOf(KeyEvent.KEYCODE_UNKNOWN)
    private var observedStylusButtons = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        incomingUri.value = intent?.incomingDocumentUri()
        incomingNotebookId.value = intent?.getStringExtra(EXTRA_NOTEBOOK_ID)
        enableEdgeToEdge()
        hideBottomNavigationBar()
        setContent {
            val settingsStore = remember { AppSettingsStore(applicationContext) }
            var appSettings by remember { mutableStateOf(settingsStore.load()) }
            val activeTheme = appSettings.activeTheme
            LaunchedEffect(appSettings) {
                RecentNotesWidgetProvider.updateAll(applicationContext)
                InkHistoryWidgetProvider.updateAll(applicationContext)
            }
            val baseDensity = LocalDensity.current
            MheenotesTheme(activeTheme) {
                val systemBarColor = androidx.compose.material3.MaterialTheme.colorScheme.background.toArgb()
                SideEffect {
                    window.statusBarColor = systemBarColor
                    WindowCompat.getInsetsController(window, window.decorView).apply {
                        isAppearanceLightStatusBars = !activeTheme.dark
                        isAppearanceLightNavigationBars = !activeTheme.dark
                    }
                }
                CompositionLocalProvider(
                    LocalDensity provides Density(
                        density = baseDensity.density * appSettings.uiScale,
                        fontScale = baseDensity.fontScale * appSettings.uiScale,
                    ),
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = androidx.compose.material3.MaterialTheme.colorScheme.background,
                    ) {
                        MheenotesEditor(
                            appSettings = appSettings,
                            initialOpenUri = incomingUri.value,
                            initialNotebookId = incomingNotebookId.value,
                            hardwareKeySequence = hardwareKeySequence.intValue,
                            hardwareKeyCode = hardwareKeyCode.intValue,
                            onAppSettingsChanged = { updated ->
                                appSettings = updated
                                settingsStore.save(updated)
                            },
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        incomingUri.value = intent.incomingDocumentUri()
        incomingNotebookId.value = intent.getStringExtra(EXTRA_NOTEBOOK_ID)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        // Capture before Compose, a dialog, or a focused text field gets a chance to consume
        // the event. This is required for Bluetooth and other physical keyboards.
        if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0 && event.keyCode != KeyEvent.KEYCODE_UNKNOWN) {
            hardwareKeyCode.intValue = event.keyCode
            hardwareKeySequence.intValue++
        }
        return super.dispatchKeyEvent(event)
    }

    override fun dispatchGenericMotionEvent(event: MotionEvent): Boolean {
        captureStylusButton(event)
        return super.dispatchGenericMotionEvent(event)
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        captureStylusButton(event)
        return super.dispatchTouchEvent(event)
    }

    private fun captureStylusButton(event: MotionEvent) {
        val pointerIsStylus = event.isFromSource(InputDevice.SOURCE_STYLUS) ||
            (0 until event.pointerCount).any { index ->
                event.getToolType(index) == MotionEvent.TOOL_TYPE_STYLUS ||
                    event.getToolType(index) == MotionEvent.TOOL_TYPE_ERASER
            }
        if (!pointerIsStylus) return
        val buttons = event.buttonState and (
            MotionEvent.BUTTON_STYLUS_PRIMARY or MotionEvent.BUTTON_STYLUS_SECONDARY or
                MotionEvent.BUTTON_SECONDARY or MotionEvent.BUTTON_TERTIARY
            )
        val newlyPressed = buttons and observedStylusButtons.inv()
        observedStylusButtons = buttons
        if (newlyPressed != 0) {
            hardwareKeyCode.intValue = -Integer.lowestOneBit(newlyPressed)
            hardwareKeySequence.intValue++
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideBottomNavigationBar()
    }

    private fun hideBottomNavigationBar() {
        WindowCompat.getInsetsController(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.navigationBars())
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    private fun Intent.incomingDocumentUri(): Uri? {
        data?.let { return it }
        if (action != Intent.ACTION_SEND) return null
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            getParcelableExtra(Intent.EXTRA_STREAM)
        }
    }

    companion object {
        const val EXTRA_NOTEBOOK_ID = "com.mheenotes.app.NOTEBOOK_ID"
    }
}
