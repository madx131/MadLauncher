package com.madlauncher.data

import org.json.JSONArray
import org.json.JSONObject

data class MultiSlot(
    val kind: String,
    val appWidgetId: Int = 0,
    val label: String = "",
    val box: GlanceBox,
)

data class MultiBoard(
    val slots: List<MultiSlot> = defaultSlots(),
    val background: Boolean = false,
    val backgroundArgb: Long = 0xFF2E3440,
    val alpha: Float = 0.88f,
) {
    fun toJson(): JSONObject {
        val obj = JSONObject()
        obj.put("background", background)
        obj.put("backgroundArgb", backgroundArgb)
        obj.put("alpha", alpha.toDouble())
        val arr = JSONArray()
        slots.forEach { slot ->
            arr.put(
                JSONObject()
                    .put("kind", slot.kind)
                    .put("appWidgetId", slot.appWidgetId)
                    .put("label", slot.label)
                    .put("x", slot.box.x.toDouble())
                    .put("y", slot.box.y.toDouble())
                    .put("w", slot.box.w.toDouble())
                    .put("h", slot.box.h.toDouble()),
            )
        }
        obj.put("slots", arr)
        return obj
    }

    companion object {
        fun defaultSlots(): List<MultiSlot> = listOf(
            MultiSlot("glance", box = stackedBox(0, 2)),
            MultiSlot("notes", box = stackedBox(1, 2)),
        )

        fun stackedBox(index: Int, count: Int): GlanceBox {
            val n = count.coerceAtLeast(1)
            val gap = 0.03f
            val height = ((1f - gap * (n + 1)) / n).coerceIn(0.16f, 0.48f)
            return GlanceBox(
                x = 0.03f,
                y = gap + index * (height + gap),
                w = 0.94f,
                h = height,
            ).clamped()
        }

        fun fromLegacy(pages: List<String>): MultiBoard {
            val kinds = pages.filter { it == "glance" || it == "notes" }.ifEmpty { listOf("glance", "notes") }
            return MultiBoard(
                slots = kinds.mapIndexed { index, kind ->
                    MultiSlot(kind = kind, box = stackedBox(index, kinds.size))
                },
            )
        }

        fun fromJson(obj: JSONObject): MultiBoard {
            val slots = buildList {
                val arr = obj.optJSONArray("slots") ?: JSONArray()
                for (i in 0 until arr.length()) {
                    val item = arr.optJSONObject(i) ?: continue
                    val kind = item.optString("kind")
                    if (kind != "glance" && kind != "notes" && kind != "app") continue
                    val id = item.optInt("appWidgetId", 0)
                    if (kind == "app" && id == 0) continue
                    add(
                        MultiSlot(
                            kind = kind,
                            appWidgetId = id,
                            label = item.optString("label"),
                            box = GlanceBox(
                                item.optDouble("x", 0.03).toFloat(),
                                item.optDouble("y", 0.03).toFloat(),
                                item.optDouble("w", 0.5).toFloat(),
                                item.optDouble("h", 0.4).toFloat(),
                            ).clamped(),
                        ),
                    )
                }
            }
            return MultiBoard(
                slots = slots,
                background = obj.optBoolean("background", false),
                backgroundArgb = obj.optLong("backgroundArgb", 0xFF2E3440),
                alpha = obj.optDouble("alpha", 0.88).toFloat().coerceIn(0.05f, 1f),
            )
        }
    }
}
