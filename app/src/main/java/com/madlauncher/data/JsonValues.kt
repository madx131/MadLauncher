package com.madlauncher.data

/**
 * JSON reader for style and language-command packs. Unit tests cannot call
 * Android's stubbed org.json, and these two files are the packs those tests cover.
 */
internal object JsonValues {
    fun parse(raw: String): Any? {
        val parser = Parser(raw)
        val value = parser.value() ?: return null
        parser.skip()
        return if (parser.end()) value else null
    }

    fun obj(value: Any?): Map<String, Any?>? {
        val map = value as? Map<*, *> ?: return null
        if (map.keys.any { it !is String }) return null
        @Suppress("UNCHECKED_CAST")
        return map as Map<String, Any?>
    }

    fun list(value: Any?): List<Any?>? = value as? List<Any?>

    fun text(value: Any?): String? = value as? String

    fun number(value: Any?): Double? = (value as? Number)?.toDouble()

    private class Parser(private val raw: String) {
        private var i = 0

        fun end(): Boolean = i >= raw.length

        fun skip() {
            while (i < raw.length && raw[i].isWhitespace()) i++
        }

        fun value(): Any? {
            skip()
            if (i >= raw.length) return null
            return when (raw[i]) {
                '{' -> objectValue()
                '[' -> arrayValue()
                '"' -> stringValue()
                't' -> literal("true", true)
                'f' -> literal("false", false)
                'n' -> literal("null", null)
                '-', in '0'..'9' -> numberValue()
                else -> null
            }
        }

        private fun objectValue(): Map<String, Any?>? {
            if (!eat('{')) return null
            val out = linkedMapOf<String, Any?>()
            skip()
            if (eat('}')) return out
            while (true) {
                skip()
                val key = stringValue() ?: return null
                skip()
                if (!eat(':')) return null
                if (end()) return null
                val before = i
                val item = value()
                if (item == null && i == before) return null
                out[key] = item
                skip()
                when {
                    eat(',') -> continue
                    eat('}') -> return out
                    else -> return null
                }
            }
        }

        private fun arrayValue(): List<Any?>? {
            if (!eat('[')) return null
            val out = ArrayList<Any?>()
            skip()
            if (eat(']')) return out
            while (true) {
                val before = i
                val item = value()
                if (item == null && i == before) return null
                out += item
                skip()
                when {
                    eat(',') -> continue
                    eat(']') -> return out
                    else -> return null
                }
            }
        }

        private fun stringValue(): String? {
            skip()
            if (!eat('"')) return null
            val out = StringBuilder()
            while (i < raw.length) {
                val c = raw[i++]
                when (c) {
                    '"' -> return out.toString()
                    '\\' -> {
                        if (i >= raw.length) return null
                        when (val esc = raw[i++]) {
                            '"', '\\', '/' -> out.append(esc)
                            'b' -> out.append('\b')
                            'f' -> out.append('\u000C')
                            'n' -> out.append('\n')
                            'r' -> out.append('\r')
                            't' -> out.append('\t')
                            'u' -> {
                                if (i + 4 > raw.length) return null
                                val hex = raw.substring(i, i + 4)
                                val code = hex.toIntOrNull(16) ?: return null
                                out.append(code.toChar())
                                i += 4
                            }
                            else -> return null
                        }
                    }
                    else -> out.append(c)
                }
            }
            return null
        }

        private fun numberValue(): Double? {
            val start = i
            if (raw[i] == '-') i++
            if (i >= raw.length || !raw[i].isDigit()) return null
            while (i < raw.length && raw[i].isDigit()) i++
            if (i < raw.length && raw[i] == '.') {
                i++
                if (i >= raw.length || !raw[i].isDigit()) return null
                while (i < raw.length && raw[i].isDigit()) i++
            }
            if (i < raw.length && (raw[i] == 'e' || raw[i] == 'E')) {
                i++
                if (i < raw.length && (raw[i] == '+' || raw[i] == '-')) i++
                if (i >= raw.length || !raw[i].isDigit()) return null
                while (i < raw.length && raw[i].isDigit()) i++
            }
            return raw.substring(start, i).toDoubleOrNull()
        }

        private fun literal(word: String, value: Any?): Any? {
            if (!raw.startsWith(word, i)) return null
            i += word.length
            return value
        }

        private fun eat(c: Char): Boolean {
            skip()
            if (i < raw.length && raw[i] == c) {
                i++
                return true
            }
            return false
        }
    }
}
