package com.mheenotes.ui.settings

import com.mheenotes.ui.res.*
import org.jetbrains.compose.resources.stringResource
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.ChevronRight
import com.composables.icons.lucide.Lucide
import com.mheenotes.ui.design.Button
import com.mheenotes.ui.design.ButtonStyle
import com.mheenotes.ui.design.Card
import com.mheenotes.ui.design.Divider
import com.mheenotes.ui.design.Icon
import com.mheenotes.ui.design.ListRow
import com.mheenotes.ui.design.M
import com.mheenotes.ui.design.Modal
import com.mheenotes.ui.design.ModalActions
import com.mheenotes.ui.design.ModalTitle
import com.mheenotes.ui.design.Motion
import com.mheenotes.ui.design.Radii
import com.mheenotes.ui.design.Text

/** Open-source notices: every bundled library and font, tap one to read its license. */
@Composable
fun LicensesModal(visible: Boolean, onDismiss: () -> Unit) {
    val c = M.colors
    var open by remember { mutableStateOf<String?>(null) }
    Modal(visible, onDismiss, width = 640.dp) {
        ModalTitle(stringResource(Res.string.licenses), stringResource(Res.string.licenses_intro))
        Card(Modifier.fillMaxWidth().heightIn(max = 420.dp), color = c.fill, border = false, shape = RoundedCornerShape(Radii.md)) {
            LazyColumn {
                items(COMPONENTS, key = { it.name }) { comp ->
                    val expanded = open == comp.name
                    val turn by animateFloatAsState(if (expanded) 90f else 0f, Motion.standard(), label = "chev")
                    ListRow(comp.name, subtitle = "${comp.owner} · ${comp.license.name}", onClick = { open = if (expanded) null else comp.name }) {
                        Icon(Lucide.ChevronRight, Modifier.rotate(turn), tint = c.textTertiary, size = 16.dp)
                    }
                    AnimatedVisibility(expanded) {
                        Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)) {
                            Text(comp.url, style = M.type.caption, color = c.accent)
                            Text(
                                comp.license.text,
                                Modifier
                                    .padding(top = 8.dp)
                                    .clip(RoundedCornerShape(Radii.sm))
                                    .background(c.surface)
                                    .horizontalScroll(rememberScrollState())
                                    .padding(12.dp),
                                style = M.type.caption.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp, lineHeight = 15.sp),
                                color = c.textSecondary,
                            )
                        }
                    }
                    Divider(Modifier.padding(start = 16.dp))
                }
            }
        }
        ModalActions { Button(stringResource(Res.string.done), onDismiss, style = ButtonStyle.Primary) }
    }
}
