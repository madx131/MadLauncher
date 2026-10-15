package com.madlauncher.data

import org.json.JSONArray
import org.json.JSONObject

enum class GlanceClockStyle { CLASSIC, THIN, STACKED, SECONDS, ANALOG, ANALOG_NUMBERS, ANALOG_MINIMAL }

data class GlanceBox(val x: Float, val y: Float, val w: Float, val h: Float) {
    fun clamped(): GlanceBox {
        val width = w.coerceIn(0.16f, 1f)
        val height = h.coerceIn(0.10f, 1f)
        return GlanceBox(
            x = x.coerceIn(0f, (1f - width).coerceAtLeast(0f)),
            y = y.coerceIn(0f, (1f - height).coerceAtLeast(0f)),
            w = width,
            h = height,
        )
    }
}

enum class GlanceDateStyle { WEEKDAY, LONG, SHORT, NUMERIC }

enum class GlanceWeatherStyle { TEXT, TEMP, CONDITION, STACKED }

enum class GlanceUnreadStyle { ICONS, COUNT, NAMES, BADGE }

enum class GlanceStatStyle { LABEL, VALUE, RING, BAR }

enum class GlanceFont { DEFAULT, SERIF, MONO }

enum class GlanceWeight { LIGHT, REGULAR, BOLD }

data class GlanceType(
    val font: GlanceFont = GlanceFont.DEFAULT,
    val weight: GlanceWeight = GlanceWeight.REGULAR,
    val scale: Float = 1f,
)

object GlanceElements {
    const val CLOCK = "clock"
    const val DATE = "date"
    const val WEATHER = "weather"
    const val UNREAD = "unread"
    const val BATTERY = "battery"
    const val CPU = "cpu"
    const val RAM = "ram"

    val all = listOf(CLOCK, DATE, WEATHER, UNREAD, BATTERY, CPU, RAM)
    val defaultOn = setOf(CLOCK, DATE, WEATHER)

    fun defaultBox(id: String): GlanceBox = when (id) {
        CLOCK -> GlanceBox(0.04f, 0.02f, 0.92f, 0.46f)
        DATE -> GlanceBox(0.08f, 0.50f, 0.84f, 0.16f)
        WEATHER -> GlanceBox(0.08f, 0.68f, 0.84f, 0.14f)
        UNREAD -> GlanceBox(0.08f, 0.84f, 0.84f, 0.14f)
        BATTERY -> GlanceBox(0.02f, 0.82f, 0.31f, 0.16f)
        CPU -> GlanceBox(0.34f, 0.82f, 0.31f, 0.16f)
        RAM -> GlanceBox(0.66f, 0.82f, 0.31f, 0.16f)
        else -> GlanceBox(0.08f, 0.08f, 0.4f, 0.2f)
    }

    fun defaultType(id: String, clock: GlanceClockStyle): GlanceType = when (id) {
        CLOCK -> GlanceType(
            weight = if (clock == GlanceClockStyle.THIN) GlanceWeight.LIGHT else GlanceWeight.BOLD,
        )
        else -> GlanceType()
    }
}

data class GlanceSettings(
    val clockStyle: GlanceClockStyle = GlanceClockStyle.CLASSIC,
    val dateStyle: GlanceDateStyle = GlanceDateStyle.LONG,
    val weatherStyle: GlanceWeatherStyle = GlanceWeatherStyle.TEXT,
    val unreadStyle: GlanceUnreadStyle = GlanceUnreadStyle.ICONS,
    val batteryStyle: GlanceStatStyle = GlanceStatStyle.LABEL,
    val cpuStyle: GlanceStatStyle = GlanceStatStyle.LABEL,
    val ramStyle: GlanceStatStyle = GlanceStatStyle.LABEL,
    val background: Boolean = false,
    val backgroundArgb: Long = 0xFF2E3440,
    val alpha: Float = 0.88f,
    val order: List<String> = GlanceElements.all,
    val enabled: Set<String> = GlanceElements.defaultOn,
    val boxes: Map<String, GlanceBox> = emptyMap(),
    val types: Map<String, GlanceType> = emptyMap(),
) {
    fun visible(): List<String> = order.filter { it in enabled }

    fun box(id: String): GlanceBox = (boxes[id] ?: GlanceElements.defaultBox(id)).clamped()

    fun typeOf(id: String): GlanceType = types[id] ?: GlanceElements.defaultType(id, clockStyle)

    fun statStyle(id: String): GlanceStatStyle = when (id) {
        GlanceElements.BATTERY -> batteryStyle
        GlanceElements.CPU -> cpuStyle
        GlanceElements.RAM -> ramStyle
        else -> GlanceStatStyle.LABEL
    }

    fun liveSeconds(): Boolean = clockStyle == GlanceClockStyle.SECONDS ||
        clockStyle == GlanceClockStyle.ANALOG ||
        clockStyle == GlanceClockStyle.ANALOG_NUMBERS

    fun toJson(): String {
        val obj = JSONObject()
        obj.put("clockStyle", clockStyle.name)
        obj.put("dateStyle", dateStyle.name)
        obj.put("weatherStyle", weatherStyle.name)
        obj.put("unreadStyle", unreadStyle.name)
        obj.put("batteryStyle", batteryStyle.name)
        obj.put("cpuStyle", cpuStyle.name)
        obj.put("ramStyle", ramStyle.name)
        obj.put("background", background)
        obj.put("backgroundArgb", backgroundArgb)
        obj.put("alpha", alpha.toDouble())
        obj.put("order", JSONArray(order))
        obj.put("enabled", JSONArray(enabled.toList()))
        val placed = JSONObject()
        boxes.forEach { (id, box) ->
            placed.put(id, JSONArray(listOf(box.x.toDouble(), box.y.toDouble(), box.w.toDouble(), box.h.toDouble())))
        }
        obj.put("boxes", placed)
        val typed = JSONObject()
        types.forEach { (id, type) ->
            typed.put(
                id,
                JSONObject()
                    .put("font", type.font.name)
                    .put("weight", type.weight.name)
                    .put("scale", type.scale.toDouble()),
            )
        }
        obj.put("types", typed)
        return obj.toString()
    }

    companion object {
        fun fromJson(raw: String?): GlanceSettings {
            if (raw.isNullOrBlank()) return GlanceSettings()
            return runCatching {
                val o = JSONObject(raw)
                val order = o.optJSONArray("order")?.let { arr ->
                    buildList {
                        for (i in 0 until arr.length()) {
                            val id = arr.optString(i)
                            if (id in GlanceElements.all) add(id)
                        }
                    }
                }.orEmpty().ifEmpty { GlanceElements.all }
                val known = order + GlanceElements.all.filter { it !in order }
                val enabled = o.optJSONArray("enabled")?.let { arr ->
                    buildSet {
                        for (i in 0 until arr.length()) {
                            val id = arr.optString(i)
                            if (id in GlanceElements.all) add(id)
                        }
                    }
                } ?: GlanceElements.defaultOn
                val types = buildMap {
                    val typed = o.optJSONObject("types")
                    if (typed != null) {
                        typed.keys().forEach { key ->
                            if (key !in GlanceElements.all) return@forEach
                            val item = typed.optJSONObject(key) ?: return@forEach
                            put(
                                key,
                                GlanceType(
                                    font = GlanceFont.entries.firstOrNull { it.name == item.optString("font") }
                                        ?: GlanceFont.DEFAULT,
                                    weight = GlanceWeight.entries.firstOrNull { it.name == item.optString("weight") }
                                        ?: GlanceWeight.REGULAR,
                                    scale = item.optDouble("scale", 1.0).toFloat().coerceIn(0.6f, 2f),
                                ),
                            )
                        }
                    }
                }
                val boxes = buildMap {
                    val placed = o.optJSONObject("boxes")
                    if (placed != null) {
                        placed.keys().forEach { key ->
                            if (key !in GlanceElements.all) return@forEach
                            val arr = placed.optJSONArray(key) ?: return@forEach
                            if (arr.length() < 4) return@forEach
                            put(
                                key,
                                GlanceBox(
                                    arr.optDouble(0).toFloat(),
                                    arr.optDouble(1).toFloat(),
                                    arr.optDouble(2).toFloat(),
                                    arr.optDouble(3).toFloat(),
                                ).clamped(),
                            )
                        }
                    }
                }
                GlanceSettings(
                    clockStyle = GlanceClockStyle.entries.firstOrNull { it.name == o.optString("clockStyle") }
                        ?: GlanceClockStyle.CLASSIC,
                    dateStyle = GlanceDateStyle.entries.firstOrNull { it.name == o.optString("dateStyle") }
                        ?: GlanceDateStyle.LONG,
                    weatherStyle = GlanceWeatherStyle.entries.firstOrNull { it.name == o.optString("weatherStyle") }
                        ?: GlanceWeatherStyle.TEXT,
                    unreadStyle = GlanceUnreadStyle.entries.firstOrNull { it.name == o.optString("unreadStyle") }
                        ?: GlanceUnreadStyle.ICONS,
                    batteryStyle = GlanceStatStyle.entries.firstOrNull { it.name == o.optString("batteryStyle") }
                        ?: GlanceStatStyle.LABEL,
                    cpuStyle = GlanceStatStyle.entries.firstOrNull { it.name == o.optString("cpuStyle") }
                        ?: GlanceStatStyle.LABEL,
                    ramStyle = GlanceStatStyle.entries.firstOrNull { it.name == o.optString("ramStyle") }
                        ?: GlanceStatStyle.LABEL,
                    background = o.optBoolean("background", false),
                    backgroundArgb = o.optLong("backgroundArgb", 0xFF2E3440),
                    alpha = o.optDouble("alpha", 0.88).toFloat().coerceIn(0.05f, 1f),
                    order = known,
                    enabled = enabled,
                    boxes = boxes,
                    types = types,
                )
            }.getOrDefault(GlanceSettings())
        }

    }
}
