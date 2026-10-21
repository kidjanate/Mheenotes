package com.mheenotes.ui

import android.content.res.Configuration
import android.content.res.Resources
import android.os.LocaleList
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import java.util.Locale

actual fun applyAppLanguage(language: AppLanguage) {
    LocaleList.setDefault(
        when (language) {
            AppLanguage.System -> Resources.getSystem().configuration.locales
            AppLanguage.English -> LocaleList(Locale.ENGLISH)
            AppLanguage.Thai -> LocaleList(Locale.forLanguageTag("th"))
        },
    )
}

@Composable
actual fun ProvideAppLanguage(language: AppLanguage, content: @Composable () -> Unit) {
    val base = LocalConfiguration.current
    // A configuration change resets the process locale, so re-apply it whenever the base changes.
    // Resources read the locale and LocalConfiguration, so a new configuration re-resolves them all.
    val config = remember(base, language) {
        applyAppLanguage(language)
        Configuration(base).apply { setLocales(LocaleList.getDefault()) }
    }
    CompositionLocalProvider(LocalConfiguration provides config, content = content)
}
