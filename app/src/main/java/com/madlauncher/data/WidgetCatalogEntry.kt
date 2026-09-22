package com.madlauncher.data

import android.appwidget.AppWidgetProviderInfo
import android.graphics.Bitmap

/**
 * One selectable widget offer in the picker. Built-ins use [builtInId];
 * third-party entries keep [info] and a unique [providerKey].
 */
data class WidgetCatalogEntry(
    val providerKey: String,
    val appLabel: String,
    val widgetLabel: String,
    val packageName: String,
    val spanX: Int,
    val spanY: Int,
    val info: AppWidgetProviderInfo? = null,
    val builtInId: String? = null,
    val preview: Bitmap? = null,
) {
    val isBuiltIn: Boolean get() = builtInId != null
}
