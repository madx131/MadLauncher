package com.madlauncher.ui

import android.content.Context
import android.content.ContextWrapper
import android.content.res.Resources
import com.madlauncher.data.LanguageOverlay

/** Context whose [Resources] resolve through [LanguageOverlay] JSON packs. */
class LanguageOverlayContextWrapper(base: Context) : ContextWrapper(base) {
    private var cached: OverlayResources? = null
    private var cachedConfigHash: Int = 0

    override fun getResources(): Resources {
        val baseRes = super.getResources()
        val hash = baseRes.configuration.hashCode()
        if (cached == null || cachedConfigHash != hash) {
            cached = OverlayResources(baseRes)
            cachedConfigHash = hash
        }
        return cached!!
    }

    @Suppress("DEPRECATION")
    class OverlayResources(
        private val base: Resources,
    ) : Resources(base.assets, base.displayMetrics, base.configuration) {

        override fun getResourceEntryName(id: Int): String = base.getResourceEntryName(id)

        override fun getString(id: Int): String =
            LanguageOverlay.wrapGetString(base, id) { base.getString(id) }

        override fun getText(id: Int): CharSequence = getString(id)

        override fun getString(id: Int, vararg formatArgs: Any?): String {
            val template = LanguageOverlay.wrapGetString(base, id) { base.getString(id) }
            return if (formatArgs.isEmpty()) {
                template
            } else {
                val locale = formatLocale()
                String.format(locale, template, *formatArgs)
            }
        }

        private fun formatLocale(): java.util.Locale {
            val code = LanguageOverlay.selectedFile.removeSuffix(".json").substringBefore('-').lowercase()
            val tag = when (code) {
                "iw", "he", "heb" -> "he"
                "ar", "ara" -> "ar"
                "fa", "fas" -> "fa"
                "ur", "urd" -> "ur"
                else -> null
            }
            return if (tag != null) java.util.Locale.forLanguageTag(tag) else base.configuration.locales[0]
        }
    }
}

fun isRtlLanguageOverlay(fileName: String): Boolean {
    if (fileName.isBlank()) return false
    val code = fileName.removeSuffix(".json").substringBefore('-').lowercase()
    return code in RTL_LANGUAGE_CODES
}

private val RTL_LANGUAGE_CODES = setOf(
    "iw", "he", "heb", "ar", "ara", "fa", "fas", "ur", "urd",
)
