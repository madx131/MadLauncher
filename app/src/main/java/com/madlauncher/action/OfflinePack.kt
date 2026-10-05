package com.madlauncher.action

import android.content.Context
import java.net.HttpURLConnection
import java.net.URL

/** Offline modules from the MadLauncher repo. Each one can be turned on by itself. */
object OfflinePack {
    const val RAW = "https://raw.githubusercontent.com/madx131/MadLauncher/main/offline/"
    const val CALC = "calc"
    const val CONVERSIONS = "conversions"
    const val CURRENCY = "currency"
    const val TRANSLATE = "translate"

    data class Module(val id: String, val name: String)

    data class Saved(val currencyJson: String)

    private val defaults = listOf(
        Module(CALC, "Calculator"),
        Module(CONVERSIONS, "Unit conversion"),
        Module(CURRENCY, "Currency"),
        Module(TRANSLATE, "Offline translation"),
    )

    fun catalog(context: Context): List<Module> {
        val json = read("manifest.json", context) ?: return defaults
        val found = Regex(""""id"\s*:\s*"([^"]+)"\s*,\s*"name"\s*:\s*"([^"]+)"""")
            .findAll(json)
            .map { Module(it.groupValues[1], it.groupValues[2]) }
            .toList()
        return found.ifEmpty { defaults }
    }

    fun download(context: Context): Result<Saved> {
        val currency = read("currency.json", context)
            ?: return Result.failure(IllegalStateException("Couldn't download currency rates"))
        if (!currency.contains("\"rates\"")) {
            return Result.failure(IllegalStateException("Currency pack was empty"))
        }
        read("manifest.json", context)
        return Result.success(Saved(currency))
    }

    private fun read(name: String, context: Context): String? {
        runCatching { httpGet(RAW + name) }.getOrNull()?.takeIf { it.isNotBlank() }?.let { return it }
        return runCatching {
            context.assets.open("offline/$name").bufferedReader().use { it.readText() }
        }.getOrNull()
    }

    private fun httpGet(url: String): String {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 8000
            readTimeout = 8000
            requestMethod = "GET"
            setRequestProperty("User-Agent", "MADActionBox/1.0")
        }
        return try {
            val code = conn.responseCode
            if (code !in 200..299) return ""
            conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }
}
