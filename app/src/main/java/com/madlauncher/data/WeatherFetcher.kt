package com.madlauncher.data

import android.Manifest
import android.content.Context
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

object WeatherFetcher {
    fun label(context: Context): String {
        if (!PermissionChecker.granted(context, Manifest.permission.ACCESS_COARSE_LOCATION)) {
            return "Weather"
        }
        val loc = DevicePlaceFinder.coordinates(context) ?: return "Weather"
        val us = Locale.getDefault().country.equals("US", ignoreCase = true)
        val unit = if (us) "fahrenheit" else "celsius"
        val suffix = if (us) "°F" else "°C"
        val url =
            "https://api.open-meteo.com/v1/forecast?latitude=${loc.first}&longitude=${loc.second}" +
                "&current=temperature_2m,weather_code&temperature_unit=$unit"
        return runCatching {
            val body = httpGet(url)
            val current = JSONObject(body).getJSONObject("current")
            val temp = current.getDouble("temperature_2m").toInt()
            val code = current.optInt("weather_code", 0)
            "${wmoLabel(code)} · $temp$suffix"
        }.getOrDefault("Weather")
    }

    private fun httpGet(url: String): String {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 8000
            readTimeout = 8000
            requestMethod = "GET"
            setRequestProperty("User-Agent", "MADLauncher/1.0")
        }
        return conn.inputStream.bufferedReader().use { it.readText() }.also { conn.disconnect() }
    }

    private fun wmoLabel(code: Int): String {
        return when (code) {
            0 -> "Clear"
            1, 2 -> "Mostly clear"
            3 -> "Cloudy"
            45, 48 -> "Fog"
            in 51..57 -> "Drizzle"
            in 61..67 -> "Rain"
            in 71..77 -> "Snow"
            in 80..82 -> "Showers"
            in 85..86 -> "Snow showers"
            in 95..99 -> "Thunder"
            else -> "Weather"
        }
    }
}
