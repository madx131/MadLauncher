package com.madlauncher.data

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.location.LocationManager
import java.util.Locale

data class DevicePlace(
    val country: String,
    val language: String,
    val city: String = "",
    val countryName: String = "",
    val fromLocation: Boolean = false,
) {
    val label: String
        get() = listOf(city, countryName.ifBlank { country }).filter { it.isNotBlank() }.distinct().joinToString(", ")
}

object DevicePlaceFinder {
    fun current(context: Context, useLocation: Boolean): DevicePlace {
        val locale = Locale.getDefault()
        val fallback = DevicePlace(
            country = locale.country.ifBlank { "US" }.uppercase(Locale.US),
            language = locale.language.ifBlank { "en" },
            countryName = locale.displayCountry,
        )
        if (!useLocation || !PermissionChecker.granted(context, Manifest.permission.ACCESS_COARSE_LOCATION)) {
            return fallback
        }
        val loc = coordinates(context) ?: return fallback
        val geo = reverse(context, loc.first, loc.second) ?: return fallback.copy(fromLocation = true)
        return geo.copy(language = fallback.language, fromLocation = true)
    }

    @SuppressLint("MissingPermission")
    fun coordinates(context: Context): Pair<Double, Double>? {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val loc = runCatching { lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER) }.getOrNull()
            ?: runCatching { lm.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER) }.getOrNull()
            ?: runCatching { lm.getLastKnownLocation(LocationManager.GPS_PROVIDER) }.getOrNull()
        return loc?.let { it.latitude to it.longitude }
    }

    @Suppress("DEPRECATION")
    private fun reverse(context: Context, lat: Double, lon: Double): DevicePlace? {
        val geocoder = runCatching { Geocoder(context, Locale.getDefault()) }.getOrNull() ?: return null
        val address = runCatching { geocoder.getFromLocation(lat, lon, 1)?.firstOrNull() }.getOrNull() ?: return null
        val country = address.countryCode?.uppercase(Locale.US).orEmpty()
        if (country.isBlank()) return null
        val city = address.locality.orEmpty()
            .ifBlank { address.subAdminArea.orEmpty() }
            .ifBlank { address.adminArea.orEmpty() }
        return DevicePlace(
            country = country,
            language = Locale.getDefault().language.ifBlank { "en" },
            city = city,
            countryName = address.countryName.orEmpty(),
            fromLocation = true,
        )
    }
}
