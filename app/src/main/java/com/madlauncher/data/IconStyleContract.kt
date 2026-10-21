package com.madlauncher.data

/**
 * Icon style a separate designer app can hand to MAD.
 *
 * The designer sends [ACTION_INSTALL] to [com.madlauncher.IconStyleInstallActivity]
 * with [android.content.Intent.EXTRA_STREAM] or `data` set to a content URI of a zip
 * (`FLAG_GRANT_READ_URI_PERMISSION`). MAD can also start the designer's activity for
 * that same action and read the URI from `RESULT_OK`.
 *
 * Zip layout (8 MB max):
 * - `style.json`
 * - optional `png` / `webp` files named by the `icons` map
 *
 * `style.json` format 1:
 * ```
 * {
 *   "format": 1,
 *   "id": "my-dogs",
 *   "name": "My dogs",
 *   "mask": {
 *     "points": [[0.5, 0.05], [0.95, 0.95], [0.05, 0.95]],
 *     "logo": { "cx": 0.5, "cy": 0.62, "scale": 0.46 }
 *   },
 *   "icons": { "com.whatsapp/.Main": "icons/whatsapp.webp" }
 * }
 * ```
 *
 * `id` is `[a-z0-9_-]{1,64}`. `points` is a closed polyline in 0–1 (3–64 points,
 * y grows downward). `logo` parks the glyph; omit it for a centered 0.66 scale.
 * Icon keys are a component short name (`pkg/.Activity`) or a package name.
 * A mask replaces MAD's built-in shape. Icons without a mask are drawn as-is.
 */
object IconStyleContract {
    const val ACTION_INSTALL = "com.madlauncher.action.INSTALL_ICON_STYLE"
    const val FORMAT = 1
    const val MAX_ZIP_BYTES = 8 * 1024 * 1024
    const val MAX_ENTRY_BYTES = 1024 * 1024
    const val MAX_ICON_PX = 512

    private val idPattern = Regex("^[a-z0-9_-]{1,64}$")

    data class Spec(
        val id: String,
        val name: String,
        val mask: Mask?,
        val icons: Map<String, String>,
    )

    data class Mask(
        val points: List<Pair<Float, Float>>,
        val logo: Logo,
    )

    data class Logo(
        val cx: Float,
        val cy: Float,
        val scale: Float,
    )

    fun parse(raw: String): Spec? {
        val obj = JsonValues.obj(JsonValues.parse(raw)) ?: return null
        val format = JsonValues.number(obj["format"])?.toInt() ?: return null
        if (format != FORMAT) return null
        val id = JsonValues.text(obj["id"]).orEmpty().trim()
        val name = JsonValues.text(obj["name"]).orEmpty().trim()
        if (!idPattern.matches(id) || name.isEmpty() || name.length > 48) return null
        if (obj.containsKey("mask") && obj["mask"] != null && obj["mask"] !is Map<*, *>) return null
        val mask = if (obj["mask"] == null) null else parseMask(JsonValues.obj(obj["mask"])) ?: return null
        val icons = parseIcons(JsonValues.obj(obj["icons"])) ?: return null
        if (mask == null && icons.isEmpty()) return null
        return Spec(id, name, mask, icons)
    }

    /** Relative zip path: no absolute path, no `.` or `..` segments. */
    fun isSafeRelativePath(path: String): Boolean {
        val name = path.replace('\\', '/').trim()
        if (name.isEmpty() || name.startsWith("/") || name.contains(':')) return false
        val parts = name.split('/')
        if (parts.any { it.isEmpty() || it == "." || it == ".." }) return false
        return true
    }

    private fun parseMask(obj: Map<String, Any?>?): Mask? {
        if (obj == null) return null
        val points = JsonValues.list(obj["points"]) ?: return null
        if (points.size !in 3..64) return null
        val parsed = ArrayList<Pair<Float, Float>>(points.size)
        for (point in points) {
            val pair = JsonValues.list(point) ?: return null
            if (pair.size < 2) return null
            val x = JsonValues.number(pair[0])?.toFloat() ?: return null
            val y = JsonValues.number(pair[1])?.toFloat() ?: return null
            if (x !in 0f..1f || y !in 0f..1f) return null
            parsed += x to y
        }
        val logoObj = JsonValues.obj(obj["logo"])
        val logo = if (logoObj == null) {
            if (obj.containsKey("logo") && obj["logo"] != null) return null
            Logo(0.5f, 0.5f, 0.66f)
        } else {
            val cx = JsonValues.number(logoObj["cx"])?.toFloat() ?: return null
            val cy = JsonValues.number(logoObj["cy"])?.toFloat() ?: return null
            val scale = JsonValues.number(logoObj["scale"])?.toFloat() ?: return null
            if (cx !in 0f..1f || cy !in 0f..1f || scale !in 0.05f..1f) return null
            Logo(cx, cy, scale)
        }
        return Mask(parsed, logo)
    }

    private fun parseIcons(obj: Map<String, Any?>?): Map<String, String>? {
        if (obj == null) return emptyMap()
        val out = linkedMapOf<String, String>()
        for ((rawKey, rawValue) in obj) {
            val key = rawKey.trim()
            val path = JsonValues.text(rawValue).orEmpty().replace('\\', '/').trim()
            if (key.isEmpty() || key.length > 200) return null
            if (!isSafeRelativePath(path)) return null
            val lower = path.lowercase()
            if (!lower.endsWith(".png") && !lower.endsWith(".webp")) return null
            out[key] = path
        }
        return out
    }
}
