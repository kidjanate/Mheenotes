package com.mheenotes.ui.editor

import androidx.compose.runtime.Composable
import com.mheenotes.core.model.FontKind
import com.mheenotes.core.model.Template
import com.mheenotes.ui.res.*
import org.jetbrains.compose.resources.stringResource

@Composable
fun templateLabel(t: Template): String = stringResource(
    when (t) {
        Template.Blank -> Res.string.tpl_blank
        Template.Lined -> Res.string.tpl_lined
        Template.Grid -> Res.string.tpl_grid
        Template.Dotted -> Res.string.tpl_dotted
        Template.Cornell -> Res.string.tpl_cornell
        Template.MusicStaff -> Res.string.tpl_music
    },
)

@Composable
fun fontLabel(f: FontKind): String = stringResource(
    when (f) {
        FontKind.Sans -> Res.string.font_sans
        FontKind.Serif -> Res.string.font_serif
        FontKind.Mono -> Res.string.font_mono
    },
)
