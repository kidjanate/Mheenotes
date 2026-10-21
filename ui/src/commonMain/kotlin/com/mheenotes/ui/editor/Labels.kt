package com.mheenotes.ui.editor

import com.mheenotes.core.model.FontKind
import com.mheenotes.core.model.Template
import com.mheenotes.ui.i18n.Strings

fun templateLabel(t: Template, s: Strings): String = when (t) {
    Template.Blank -> s.tplBlank
    Template.Lined -> s.tplLined
    Template.Grid -> s.tplGrid
    Template.Dotted -> s.tplDotted
    Template.Cornell -> s.tplCornell
    Template.MusicStaff -> s.tplMusic
}

fun fontLabel(f: FontKind, s: Strings): String = when (f) {
    FontKind.Sans -> s.fontSans
    FontKind.Serif -> s.fontSerif
    FontKind.Mono -> s.fontMono
}
