package com.mheenotes.ui.design

import android.content.res.Resources
import android.graphics.Typeface
import android.graphics.fonts.Font
import android.graphics.fonts.FontFamily as PlatformFamily
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import com.mheenotes.ui.R

private var cached: AppFonts? = null

@Composable
actual fun rememberAppFonts(): AppFonts {
    val res = LocalContext.current.resources
    return remember { cached ?: build(res).also { cached = it } }
}

private fun build(res: Resources): AppFonts {
    fun family(latin: Int, latinExt: Int, thai: Int): FontFamily {
        fun fam(id: Int) = PlatformFamily.Builder(Font.Builder(res, id).build()).build()
        // Per-glyph fallback: Inter Latin, then Inter Latin Extended, then IBM Plex Sans Thai.
        val tf = Typeface.CustomFallbackBuilder(fam(latin))
            .addCustomFallback(fam(latinExt))
            .addCustomFallback(fam(thai))
            .setSystemFallback("sans-serif")
            .build()
        return FontFamily(androidx.compose.ui.text.font.Typeface(tf))
    }
    return AppFonts(
        regular = family(R.font.inter_400, R.font.inter_ext_400, R.font.plexthai_400),
        medium = family(R.font.inter_500, R.font.inter_ext_500, R.font.plexthai_500),
        semibold = family(R.font.inter_600, R.font.inter_ext_600, R.font.plexthai_600),
        bold = family(R.font.inter_700, R.font.inter_ext_700, R.font.plexthai_700),
    )
}
