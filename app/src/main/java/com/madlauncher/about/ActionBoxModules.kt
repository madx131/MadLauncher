package com.madlauncher.about

import androidx.annotation.StringRes
import com.madlauncher.R

/** A piece the Action Box uses, with the page for that source when there is one. */
data class ActionModule(
    @StringRes val name: Int,
    @StringRes val role: Int,
    @StringRes val source: Int,
    val url: String,
)

object ActionBoxModules {
    val all: List<ActionModule> = listOf(
        ActionModule(
            name = R.string.about_module_calc,
            role = R.string.about_module_calc_role,
            source = R.string.about_module_calc,
            url = "",
        ),
        ActionModule(
            name = R.string.about_module_conversions,
            role = R.string.about_module_conversions_role,
            source = R.string.about_module_conversions,
            url = "",
        ),
        ActionModule(
            name = R.string.about_module_currency,
            role = R.string.about_module_currency_role,
            source = R.string.about_module_currency_source,
            url = "https://www.frankfurter.app/",
        ),
        ActionModule(
            name = R.string.about_module_fuel,
            role = R.string.about_module_fuel_role,
            source = R.string.about_module_fuel_source,
            url = "https://openvan.camp/",
        ),
        ActionModule(
            name = R.string.about_module_instant,
            role = R.string.about_module_instant_role,
            source = R.string.about_module_instant_source,
            url = "https://duckduckgo.com/api",
        ),
        ActionModule(
            name = R.string.about_module_bangs,
            role = R.string.about_module_bangs_role,
            source = R.string.about_module_bangs_source,
            url = "https://duckduckgo.com/bangs",
        ),
        ActionModule(
            name = R.string.about_module_translate,
            role = R.string.about_module_translate_role,
            source = R.string.about_module_translate_source,
            url = "https://github.com/googlesamples/mlkit",
        ),
    )
}
