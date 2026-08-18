package com.mheenotes.app.ui

import android.view.KeyEvent
import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import com.mheenotes.core.model.InkTool
import org.json.JSONArray
import org.json.JSONObject

data class AppThemePreset(
    val id: String,
    val name: String,
    val dark: Boolean,
    val primaryArgb: Int,
    val secondaryArgb: Int,
    val backgroundArgb: Int,
    val surfaceArgb: Int,
)

enum class ActionActivationMode { Explicit, PauseAfterStroke, Disabled }

data class AppSettings(
    val themes: List<AppThemePreset> = defaultThemePresets,
    val selectedThemeId: String = defaultThemePresets.first().id,
    val penMaximumWidthMicrons: Float = 5_000f,
    val pencilMaximumWidthMicrons: Float = 5_000f,
    val highlighterMaximumWidthMicrons: Float = 5_000f,
    val eraserMaximumWidthMicrons: Float = 5_000f,
    val penWidthPresets: List<Float> = listOf(700f, 1_400f, 2_800f),
    val pencilWidthPresets: List<Float> = listOf(600f, 1_200f, 2_000f),
    val highlighterWidthPresets: List<Float> = listOf(2_500f, 4_000f, 5_000f),
    val penColorPresets: List<Int> = listOf(0xFF111827.toInt(), 0xFF2563EB.toInt(), 0xFFD43D3D.toInt()),
    val pencilColorPresets: List<Int> = listOf(0xFF30343B.toInt(), 0xFF68707C.toInt(), 0xFF315C8C.toInt()),
    val highlighterColorPresets: List<Int> = listOf(0xFFF7D34A.toInt(), 0xFF75E6A4.toInt(), 0xFFFF8FC7.toInt()),
    val actionActivationMode: ActionActivationMode = ActionActivationMode.Explicit,
    val drawAndHoldEnabled: Boolean = true,
    val floatingNotesKeyCode: Int = KeyEvent.KEYCODE_F8,
    val radialMenuKeyCode: Int = KeyEvent.KEYCODE_UNKNOWN,
    val hoverShortcutEnabled: Boolean = true,
    val hoverTapImpulseThresholdDp: Float = 2.2f,
    val hoverTapMaximumDriftDp: Float = 28f,
    val hoverTapMaximumIntervalMillis: Int = 520,
    val toolbarDock: String = "Top",
    val uiScale: Float = 1f,
    val askBeforeInsertingPage: Boolean = true,
    val customFonts: List<String> = emptyList(),
) {
    val activeTheme: AppThemePreset
        get() = themes.firstOrNull { it.id == selectedThemeId } ?: themes.first()

    fun widthPresets(tool: InkTool): List<Float> = when (tool) {
        InkTool.Pen -> penWidthPresets
        InkTool.Pencil -> pencilWidthPresets
        InkTool.Highlighter -> highlighterWidthPresets
        InkTool.Eraser -> emptyList()
    }

    fun maximumWidthMicrons(tool: InkTool): Float = when (tool) {
        InkTool.Pen -> penMaximumWidthMicrons
        InkTool.Pencil -> pencilMaximumWidthMicrons
        InkTool.Highlighter -> highlighterMaximumWidthMicrons
        InkTool.Eraser -> eraserMaximumWidthMicrons
    }

    fun withMaximumWidthMicrons(tool: InkTool, value: Float): AppSettings {
        val maximum = value.coerceIn(1_000f, 40_000f)
        return when (tool) {
            InkTool.Pen -> copy(penMaximumWidthMicrons = maximum)
            InkTool.Pencil -> copy(pencilMaximumWidthMicrons = maximum)
            InkTool.Highlighter -> copy(highlighterMaximumWidthMicrons = maximum)
            InkTool.Eraser -> copy(eraserMaximumWidthMicrons = maximum)
        }
    }

    fun withWidthPresets(tool: InkTool, values: List<Float>): AppSettings = when (tool) {
        InkTool.Pen -> copy(penWidthPresets = values)
        InkTool.Pencil -> copy(pencilWidthPresets = values)
        InkTool.Highlighter -> copy(highlighterWidthPresets = values)
        InkTool.Eraser -> this
    }

    fun colorPresets(tool: InkTool): List<Int> = when (tool) {
        InkTool.Pen -> penColorPresets
        InkTool.Pencil -> pencilColorPresets
        InkTool.Highlighter -> highlighterColorPresets
        InkTool.Eraser -> emptyList()
    }

    fun withColorPresets(tool: InkTool, values: List<Int>): AppSettings = when (tool) {
        InkTool.Pen -> copy(penColorPresets = values)
        InkTool.Pencil -> copy(pencilColorPresets = values)
        InkTool.Highlighter -> copy(highlighterColorPresets = values)
        InkTool.Eraser -> this
    }
}

// Popular-theme names are based on the default most-installed catalog at vscodethemes.com.
// These are small Mheenotes-native palettes, not bundled VS Code extensions or copied theme files.
val defaultThemePresets = listOf(
    theme("github-dark", "GitHub Dark", true, 0xFF58A6FF, 0xFFBC8CFF, 0xFF0D1117, 0xFF161B22),
    theme("one-dark-pro", "One Dark Pro", true, 0xFF61AFEF, 0xFFC678DD, 0xFF1E2127, 0xFF282C34),
    theme("dracula", "Dracula", true, 0xFFBD93F9, 0xFFFF79C6, 0xFF191A21, 0xFF282A36),
    theme("atom-one-dark", "Atom One Dark", true, 0xFF61AFEF, 0xFF98C379, 0xFF21252B, 0xFF282C34),
    theme("material-ocean", "Material Ocean", true, 0xFF82AAFF, 0xFFC792EA, 0xFF0F111A, 0xFF1A1C25),
    theme("ayu-mirage", "Ayu Mirage", true, 0xFFFFCC66, 0xFF73D0FF, 0xFF1F2430, 0xFF242936),
    theme("monokai-pro", "Monokai Pro", true, 0xFFA9DC76, 0xFFFF6188, 0xFF221F22, 0xFF2D2A2E),
    theme("winter-is-coming", "Winter is Coming", true, 0xFF82AAFF, 0xFFC792EA, 0xFF011627, 0xFF102A43),
    theme("night-owl", "Night Owl", true, 0xFF82AAFF, 0xFFC792EA, 0xFF011627, 0xFF0B2942),
    theme("one-monokai", "One Monokai", true, 0xFFA6E22E, 0xFFF92672, 0xFF1E1E1E, 0xFF272822),
    theme("palenight", "Palenight", true, 0xFF82AAFF, 0xFFC792EA, 0xFF292D3E, 0xFF34384C),
    theme("synthwave-84", "SynthWave '84", true, 0xFFFF7EDB, 0xFF36F9F6, 0xFF241B2F, 0xFF2A2139),
    theme("tokyo-night", "Tokyo Night", true, 0xFF7AA2F7, 0xFFBB9AF7, 0xFF16161E, 0xFF1A1B26),
    theme("shades-of-purple", "Shades of Purple", true, 0xFFFFC600, 0xFFB362FF, 0xFF1E1E3F, 0xFF2D2B55),
    theme("cobalt2", "Cobalt2", true, 0xFFFFC600, 0xFF0088FF, 0xFF122738, 0xFF193549),
    theme("andromeda", "Andromeda", true, 0xFF00E8C6, 0xFFFF00AA, 0xFF1E2029, 0xFF23262E),
    theme("atom-one-light", "Atom One Light", false, 0xFF4078F2, 0xFFA626A4, 0xFFFAFAFA, 0xFFF0F0F0),
    theme("noctis", "Noctis", true, 0xFF49E9A6, 0xFFC5A3FF, 0xFF092D38, 0xFF0F3A45),
    theme("bearded", "Bearded", true, 0xFF4FA6ED, 0xFFC98BDF, 0xFF171C24, 0xFF222A35),
    theme("panda", "Panda", true, 0xFF19F9D8, 0xFFFF75B5, 0xFF292A2B, 0xFF313234),
    theme("nord", "Nord", true, 0xFF88C0D0, 0xFFB48EAD, 0xFF2E3440, 0xFF3B4252),
    theme("omni", "Omni", true, 0xFFFF79C6, 0xFF8BE9FD, 0xFF191622, 0xFF211C2F),
    theme("darcula", "Darcula", true, 0xFF6897BB, 0xFFCC7832, 0xFF1E1F22, 0xFF2B2B2B),
    theme("gruvbox", "Gruvbox", true, 0xFFFABD2F, 0xFFB8BB26, 0xFF1D2021, 0xFF282828),
    theme("catppuccin-mocha", "Catppuccin Mocha", true, 0xFF89B4FA, 0xFFCBA6F7, 0xFF11111B, 0xFF1E1E2E),
    theme("eva-dark", "Eva Dark", true, 0xFF56B6C2, 0xFFC678DD, 0xFF20242C, 0xFF2A2F3A),
    theme("jellyfish", "JellyFish", true, 0xFF4EC9B0, 0xFFFF6B9D, 0xFF161A1D, 0xFF1E2529),
    theme("vue", "Vue", true, 0xFF42B883, 0xFF35495E, 0xFF171C24, 0xFF202731),
    theme("ruby", "Ruby", true, 0xFFCC342D, 0xFFF5A9B8, 0xFF1F171A, 0xFF2B2024),
    theme("monokai-night", "Monokai Night", true, 0xFFA6E22E, 0xFF66D9EF, 0xFF171717, 0xFF232323),
)

private fun theme(
    id: String,
    name: String,
    dark: Boolean,
    primary: Long,
    secondary: Long,
    background: Long,
    surface: Long,
) = AppThemePreset(id, name, dark, primary.toInt(), secondary.toInt(), background.toInt(), surface.toInt())

class AppSettingsStore(context: Context) {
    private val preferences = context.getSharedPreferences("mheenotes-app-settings", Context.MODE_PRIVATE)

    fun load(): AppSettings = runCatching {
        val stored = preferences.getString("themes", null)
        val themes = if (stored == null) defaultThemePresets else JSONArray(stored).let { array ->
            List(array.length()) { index ->
                array.getJSONObject(index).let { value ->
                    AppThemePreset(
                        id = value.getString("id"),
                        name = value.getString("name"),
                        dark = value.getBoolean("dark"),
                        primaryArgb = value.getInt("primary"),
                        secondaryArgb = value.getInt("secondary"),
                        backgroundArgb = value.getInt("background"),
                        surfaceArgb = value.getInt("surface"),
                    )
                }
            }.ifEmpty { defaultThemePresets }
        }
        val legacyMaximumWidth = preferences.getFloat("maximumStrokeWidthMicrons", 5_000f)
            .coerceIn(1_000f, 40_000f)
        AppSettings(
            themes = themes,
            selectedThemeId = preferences.getString("selectedTheme", themes.first().id) ?: themes.first().id,
            penMaximumWidthMicrons = preferences.getFloat("penMaximumWidthMicrons", legacyMaximumWidth)
                .coerceIn(1_000f, 40_000f),
            pencilMaximumWidthMicrons = preferences.getFloat("pencilMaximumWidthMicrons", legacyMaximumWidth)
                .coerceIn(1_000f, 40_000f),
            highlighterMaximumWidthMicrons = preferences.getFloat("highlighterMaximumWidthMicrons", legacyMaximumWidth)
                .coerceIn(1_000f, 40_000f),
            eraserMaximumWidthMicrons = preferences.getFloat("eraserMaximumWidthMicrons", legacyMaximumWidth)
                .coerceIn(1_000f, 40_000f),
            penWidthPresets = preferences.widthPresets("penWidthPresets", listOf(700f, 1_400f, 2_800f)),
            pencilWidthPresets = preferences.widthPresets("pencilWidthPresets", listOf(600f, 1_200f, 2_000f)),
            highlighterWidthPresets = preferences.widthPresets("highlighterWidthPresets", listOf(2_500f, 4_000f, 5_000f)),
            penColorPresets = preferences.colorPresets("penColorPresets", listOf(0xFF111827.toInt(), 0xFF2563EB.toInt(), 0xFFD43D3D.toInt())),
            pencilColorPresets = preferences.colorPresets("pencilColorPresets", listOf(0xFF30343B.toInt(), 0xFF68707C.toInt(), 0xFF315C8C.toInt())),
            highlighterColorPresets = preferences.colorPresets("highlighterColorPresets", listOf(0xFFF7D34A.toInt(), 0xFF75E6A4.toInt(), 0xFFFF8FC7.toInt())),
            actionActivationMode = preferences.getString("actionActivationMode", null)
                ?.let { runCatching { ActionActivationMode.valueOf(it) }.getOrNull() }
                ?: ActionActivationMode.Explicit,
            drawAndHoldEnabled = preferences.getBoolean("drawAndHoldEnabled", true),
            floatingNotesKeyCode = preferences.getInt("floatingNotesKeyCode", KeyEvent.KEYCODE_F8),
            radialMenuKeyCode = preferences.getInt("radialMenuKeyCode", KeyEvent.KEYCODE_UNKNOWN),
            hoverShortcutEnabled = preferences.getBoolean("hoverShortcutEnabled", true),
            hoverTapImpulseThresholdDp = preferences.getFloat("hoverTapImpulseThresholdDp", 2.2f).coerceIn(.7f, 14f),
            hoverTapMaximumDriftDp = preferences.getFloat("hoverTapMaximumDriftDp", 28f).coerceIn(12f, 56f),
            hoverTapMaximumIntervalMillis = preferences.getInt("hoverTapMaximumIntervalMillis", 520).coerceIn(300, 800),
            toolbarDock = preferences.getString("toolbarDock", "Top") ?: "Top",
            uiScale = preferences.getFloat("uiScale", 1f).coerceIn(.75f, 1.35f),
            askBeforeInsertingPage = preferences.getBoolean("askBeforeInsertingPage", true),
            customFonts = preferences.getString("customFonts", null)
                ?.split('\u001F')?.filter(String::isNotBlank).orEmpty(),
        )
    }.getOrDefault(AppSettings())

    fun save(settings: AppSettings) {
        val themes = JSONArray().apply {
            settings.themes.forEach { preset ->
                put(JSONObject().apply {
                    put("id", preset.id)
                    put("name", preset.name)
                    put("dark", preset.dark)
                    put("primary", preset.primaryArgb)
                    put("secondary", preset.secondaryArgb)
                    put("background", preset.backgroundArgb)
                    put("surface", preset.surfaceArgb)
                })
            }
        }
        preferences.edit()
            .putString("themes", themes.toString())
            .putString("selectedTheme", settings.selectedThemeId)
            .putFloat("penMaximumWidthMicrons", settings.penMaximumWidthMicrons)
            .putFloat("pencilMaximumWidthMicrons", settings.pencilMaximumWidthMicrons)
            .putFloat("highlighterMaximumWidthMicrons", settings.highlighterMaximumWidthMicrons)
            .putFloat("eraserMaximumWidthMicrons", settings.eraserMaximumWidthMicrons)
            .putString("penWidthPresets", settings.penWidthPresets.joinToString(","))
            .putString("pencilWidthPresets", settings.pencilWidthPresets.joinToString(","))
            .putString("highlighterWidthPresets", settings.highlighterWidthPresets.joinToString(","))
            .putString("penColorPresets", settings.penColorPresets.joinToString(","))
            .putString("pencilColorPresets", settings.pencilColorPresets.joinToString(","))
            .putString("highlighterColorPresets", settings.highlighterColorPresets.joinToString(","))
            .putString("actionActivationMode", settings.actionActivationMode.name)
            .putBoolean("drawAndHoldEnabled", settings.drawAndHoldEnabled)
            .putInt("floatingNotesKeyCode", settings.floatingNotesKeyCode)
            .putInt("radialMenuKeyCode", settings.radialMenuKeyCode)
            .putBoolean("hoverShortcutEnabled", settings.hoverShortcutEnabled)
            .putFloat("hoverTapImpulseThresholdDp", settings.hoverTapImpulseThresholdDp)
            .putFloat("hoverTapMaximumDriftDp", settings.hoverTapMaximumDriftDp)
            .putInt("hoverTapMaximumIntervalMillis", settings.hoverTapMaximumIntervalMillis)
            .putString("toolbarDock", settings.toolbarDock)
            .putFloat("uiScale", settings.uiScale)
            .putBoolean("askBeforeInsertingPage", settings.askBeforeInsertingPage)
            .putString("customFonts", settings.customFonts.joinToString("\u001F"))
            .apply()
    }
}

private fun android.content.SharedPreferences.widthPresets(key: String, fallback: List<Float>): List<Float> =
    getString(key, null)?.split(',')?.mapNotNull(String::toFloatOrNull)?.takeIf { it.size == 3 } ?: fallback

private fun android.content.SharedPreferences.colorPresets(key: String, fallback: List<Int>): List<Int> =
    getString(key, null)?.split(',')?.mapNotNull(String::toIntOrNull)?.takeIf { it.size == 3 } ?: fallback

private fun readableOn(color: Color): Color = if (color.luminance() > .48f) Color(0xFF111318) else Color.White

@Composable
fun MheenotesTheme(preset: AppThemePreset, content: @Composable () -> Unit) {
    val primary = Color(preset.primaryArgb)
    val secondary = Color(preset.secondaryArgb)
    val background = Color(preset.backgroundArgb)
    val surface = Color(preset.surfaceArgb)
    val scheme = if (preset.dark) darkColorScheme(
        primary = primary,
        onPrimary = readableOn(primary),
        primaryContainer = primary.copy(alpha = .26f).compositeOver(surface),
        onPrimaryContainer = readableOn(surface),
        secondary = secondary,
        onSecondary = readableOn(secondary),
        secondaryContainer = secondary.copy(alpha = .24f).compositeOver(surface),
        background = background,
        onBackground = Color(0xFFF1F3F7),
        surface = surface,
        surfaceVariant = Color.White.copy(alpha = .06f).compositeOver(surface),
        onSurface = Color(0xFFF1F3F7),
        onSurfaceVariant = Color(0xFFBBC2CD),
        outline = Color.White.copy(alpha = .20f).compositeOver(surface),
    ) else lightColorScheme(
        primary = primary,
        onPrimary = readableOn(primary),
        primaryContainer = primary.copy(alpha = .18f).compositeOver(surface),
        onPrimaryContainer = Color(0xFF171A20),
        secondary = secondary,
        onSecondary = readableOn(secondary),
        secondaryContainer = secondary.copy(alpha = .16f).compositeOver(surface),
        background = background,
        onBackground = Color(0xFF171A20),
        surface = surface,
        surfaceVariant = Color.Black.copy(alpha = .06f).compositeOver(surface),
        onSurface = Color(0xFF171A20),
        onSurfaceVariant = Color(0xFF535A64),
        outline = Color.Black.copy(alpha = .22f).compositeOver(surface),
    )
    MaterialTheme(colorScheme = scheme, content = content)
}
