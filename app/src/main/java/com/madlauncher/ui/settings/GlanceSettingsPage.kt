package com.madlauncher.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.madlauncher.R
import com.madlauncher.data.GlanceClockStyle
import com.madlauncher.data.GlanceDateStyle
import com.madlauncher.data.GlanceElements
import com.madlauncher.data.GlanceFont
import com.madlauncher.data.GlanceType
import com.madlauncher.data.GlanceWeight
import com.madlauncher.data.LauncherViewModel
import com.madlauncher.ui.GlancePreview
import com.madlauncher.ui.MultiPreview
import com.madlauncher.ui.glanceElementShort
import com.madlauncher.ui.theme.LocalMadTheme

@Composable
fun GlanceSettingsPage(vm: LauncherViewModel) {
    val s = vm.glanceSettings
    val c = LocalMadTheme.current.colors
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(stringResource(R.string.glance_preview), color = c.fg, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        GlancePreview(vm, Modifier.fillMaxWidth().height(210.dp))
        Text(stringResource(R.string.glance_preview_hint), color = c.muted, fontSize = 12.sp)
        Column(
            modifier = Modifier
                .weight(1f, fill = true)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(stringResource(R.string.glance_clock_style), color = c.fg, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            SettingsStyleRow(
                options = listOf(
                    GlanceClockStyle.CLASSIC to R.string.glance_clock_classic,
                    GlanceClockStyle.THIN to R.string.glance_clock_thin,
                    GlanceClockStyle.STACKED to R.string.glance_clock_stacked,
                    GlanceClockStyle.SECONDS to R.string.glance_clock_seconds,
                    GlanceClockStyle.ANALOG to R.string.glance_clock_analog,
                    GlanceClockStyle.ANALOG_NUMBERS to R.string.glance_clock_analog_numbers,
                    GlanceClockStyle.ANALOG_MINIMAL to R.string.glance_clock_analog_minimal,
                ),
                selected = s.clockStyle,
                onPick = { style -> vm.patchGlance { it.copy(clockStyle = style) } },
            )
            Text(stringResource(R.string.glance_date_style), color = c.fg, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            SettingsStyleRow(
                options = listOf(
                    GlanceDateStyle.WEEKDAY to R.string.glance_date_weekday,
                    GlanceDateStyle.LONG to R.string.glance_date_long,
                    GlanceDateStyle.SHORT to R.string.glance_date_short,
                    GlanceDateStyle.NUMERIC to R.string.glance_date_numeric,
                ),
                selected = s.dateStyle,
                onPick = { style -> vm.patchGlance { it.copy(dateStyle = style) } },
            )
            Text(stringResource(R.string.glance_elements), color = c.fg, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            s.order.forEach { id ->
                val on = id in s.enabled
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(glanceElementLabel(id), color = c.fg, fontSize = 15.sp, modifier = Modifier.weight(1f))
                    Switch(checked = on, onCheckedChange = { vm.setGlanceElement(id, it) })
                }
                if (on) {
                    ElementTypeControls(type = s.typeOf(id)) { next ->
                        vm.patchGlance { it.copy(types = it.types + (id to next)) }
                    }
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.glance_background), color = c.fg, fontSize = 15.sp)
                    Text(stringResource(R.string.glance_background_sub), color = c.muted, fontSize = 12.sp)
                }
                Switch(checked = s.background, onCheckedChange = { on -> vm.patchGlance { it.copy(background = on) } })
            }
            if (s.background) {
                AccentColorPicker(
                    selectedArgb = s.backgroundArgb,
                    onPick = { argb -> vm.patchGlance { it.copy(backgroundArgb = argb) } },
                    title = stringResource(R.string.glance_background),
                    subtitle = stringResource(R.string.glance_background_sub),
                )
            }
            Text(stringResource(R.string.glance_alpha), color = c.fg, fontSize = 14.sp)
            Slider(
                value = s.alpha,
                onValueChange = { value -> vm.patchGlance { it.copy(alpha = value) } },
                valueRange = 0.05f..1f,
            )
        }
    }
}

@Composable
fun MultiWidgetSettingsPage(vm: LauncherViewModel) {
    val c = LocalMadTheme.current.colors
    val items = vm.homePages.flatMap { it.items }.filter { it.widget == "multi" }
    if (items.isEmpty()) {
        Text(stringResource(R.string.glance_multi_empty), color = c.muted, fontSize = 13.sp)
        return
    }
    items.forEach { item ->
        val board = vm.multiBoardFor(item.id)
        Text(stringResource(R.string.glance_preview), color = c.fg, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        MultiPreview(vm, item.id, Modifier.fillMaxWidth().height(220.dp))
        Text(stringResource(R.string.glance_preview_hint), color = c.muted, fontSize = 12.sp)
        board.slots.forEachIndexed { index, slot ->
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(multiSlotLabel(slot), color = c.fg, fontSize = 15.sp, modifier = Modifier.weight(1f))
                Text(
                    stringResource(R.string.glance_page_remove),
                    color = c.primary,
                    modifier = Modifier.clickable { vm.removeMultiSlot(item.id, index) }.padding(8.dp),
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (board.slots.none { it.kind == "glance" }) {
                AddChip(stringResource(R.string.glance_page_glance)) { vm.addMultiBuiltIn(item.id, "glance") }
            }
            if (board.slots.none { it.kind == "notes" }) {
                AddChip(stringResource(R.string.glance_page_notes)) { vm.addMultiBuiltIn(item.id, "notes") }
            }
        }
        Text(stringResource(R.string.glance_third_party), color = c.fg, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        LaunchedEffect(item.id) { vm.ensureWidgetCatalog() }
        val installed = vm.widgetCatalog.filter { !it.isBuiltIn }
        if (vm.widgetCatalogLoading && installed.isEmpty()) {
            Text(stringResource(R.string.glance_third_party_loading), color = c.muted, fontSize = 13.sp)
        } else if (installed.isEmpty()) {
            Text(stringResource(R.string.glance_third_party_empty), color = c.muted, fontSize = 13.sp)
        } else {
            installed.forEach { entry ->
                Text(
                    "${entry.appLabel} · ${entry.widgetLabel}",
                    color = c.fg,
                    fontSize = 15.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { vm.addAppWidgetToMulti(item.id, entry) }
                        .padding(vertical = 8.dp),
                )
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.glance_background), color = c.fg, fontSize = 15.sp)
                Text(stringResource(R.string.glance_background_sub), color = c.muted, fontSize = 12.sp)
            }
            Switch(
                checked = board.background,
                onCheckedChange = { on -> vm.patchMulti(item.id) { it.copy(background = on) } },
            )
        }
        if (board.background) {
            AccentColorPicker(
                selectedArgb = board.backgroundArgb,
                onPick = { argb -> vm.patchMulti(item.id) { it.copy(backgroundArgb = argb) } },
                title = stringResource(R.string.glance_background),
                subtitle = stringResource(R.string.glance_background_sub),
            )
        }
        Text(stringResource(R.string.glance_alpha), color = c.fg, fontSize = 14.sp)
        Slider(
            value = board.alpha,
            onValueChange = { value -> vm.patchMulti(item.id) { it.copy(alpha = value) } },
            valueRange = 0.05f..1f,
        )
    }
}

@Composable
private fun multiSlotLabel(slot: com.madlauncher.data.MultiSlot): String = when (slot.kind) {
    "notes" -> stringResource(R.string.glance_page_notes)
    "app" -> slot.label.ifBlank { stringResource(R.string.glance_add_widget) }
    else -> stringResource(R.string.glance_page_glance)
}

@Composable
private fun ElementTypeControls(type: GlanceType, onChange: (GlanceType) -> Unit) {
    val c = LocalMadTheme.current.colors
    Text(stringResource(R.string.glance_font), color = c.muted, fontSize = 12.sp)
    SettingsStyleRow(
        options = listOf(
            GlanceFont.DEFAULT to R.string.glance_font_default,
            GlanceFont.SERIF to R.string.glance_font_serif,
            GlanceFont.MONO to R.string.glance_font_mono,
        ),
        selected = type.font,
        onPick = { font -> onChange(type.copy(font = font)) },
    )
    SettingsStyleRow(
        options = listOf(
            GlanceWeight.LIGHT to R.string.glance_weight_light,
            GlanceWeight.REGULAR to R.string.glance_weight_regular,
            GlanceWeight.BOLD to R.string.glance_weight_bold,
        ),
        selected = type.weight,
        onPick = { weight -> onChange(type.copy(weight = weight)) },
    )
    Text(stringResource(R.string.glance_font_size), color = c.muted, fontSize = 12.sp)
    Slider(
        value = type.scale,
        onValueChange = { value -> onChange(type.copy(scale = value)) },
        valueRange = 0.6f..2f,
    )
}

@Composable
private fun AddChip(label: String, onClick: () -> Unit) {
    val c = LocalMadTheme.current.colors
    Text(
        label,
        color = c.fg,
        fontSize = 13.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
    )
}

@Composable
private fun glanceElementLabel(id: String): String = when (id) {
    GlanceElements.CLOCK -> stringResource(R.string.glance_element_clock)
    GlanceElements.DATE -> stringResource(R.string.glance_element_date)
    GlanceElements.WEATHER -> stringResource(R.string.glance_element_weather)
    GlanceElements.UNREAD -> stringResource(R.string.glance_element_unread)
    GlanceElements.BATTERY -> stringResource(R.string.glance_element_battery)
    GlanceElements.CPU -> stringResource(R.string.glance_element_cpu)
    GlanceElements.RAM -> stringResource(R.string.glance_element_ram)
    else -> glanceElementShort(id)
}
