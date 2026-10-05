package com.madlauncher.action

import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Locale

/**
 * Live numbers that stay in the Action Box: currency (DuckDuckGo / XE) and fuel prices
 * for the phone's country, in that country's currency.
 * A saved rate table covers currency when the offline pack is on and the network is down.
 */
object LiveRates {
    enum class Kind { CURRENCY, FUEL, OTHER }

    @Volatile
    var offlineRatesJson: String? = null

    /** ISO country from the phone's location. Fuel prices use this. */
    @Volatile
    var priceCountry: String = Locale.getDefault().country.ifBlank { "US" }.uppercase(Locale.US)

    /** City and country shown on the price card. */
    @Volatile
    var priceLabel: String = ""

    /** Off when the search engine is not DuckDuckGo. Spice rates are skipped. */
    @Volatile
    var useDuckDuckGo: Boolean = true

    private val names = mapOf(
        "usd" to "USD", "dollar" to "USD", "dollars" to "USD", "us" to "USD",
        "eur" to "EUR", "euro" to "EUR", "euros" to "EUR",
        "gbp" to "GBP", "pound" to "GBP", "pounds" to "GBP", "sterling" to "GBP",
        "ils" to "ILS", "nis" to "ILS", "shekel" to "ILS", "shekels" to "ILS",
        "jpy" to "JPY", "yen" to "JPY",
        "cny" to "CNY", "yuan" to "CNY", "rmb" to "CNY",
        "aud" to "AUD", "cad" to "CAD", "chf" to "CHF", "inr" to "INR",
        "rub" to "RUB", "uah" to "UAH", "try" to "TRY", "brl" to "BRL",
        "mxn" to "MXN", "krw" to "KRW", "sek" to "SEK", "nok" to "NOK",
        "dkk" to "DKK", "pln" to "PLN", "thb" to "THB", "sgd" to "SGD",
        "hkd" to "HKD", "nzd" to "NZD", "zar" to "ZAR",
    )

    fun classify(query: String): Kind {
        if (currencyRequest(query) != null) return Kind.CURRENCY
        if (isFuelQuery(query)) return Kind.FUEL
        return Kind.OTHER
    }

    fun currency(query: String): InstantAnswer? {
        val request = currencyRequest(query) ?: return null
        spice(request)?.let { return it }
        frankfurter(request)?.let { return it }
        return offline(request)
    }

    fun fuel(query: String): InstantAnswer? {
        if (!isFuelQuery(query)) return null
        val country = priceCountry.trim().uppercase(Locale.US).ifBlank { return null }
        val body = runCatching {
            httpGet("https://openvan.camp/api/fuel/prices?country=$country")
        }.getOrNull() ?: return null
        return parseRegionalFuel(query.trim(), priceLabel, body)
    }

    /** In-box translation. Returns the translated text, not a page to open. */
    fun translate(text: String, fromCode: String?, toCode: String?): String? {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return null
        val sl = tag(fromCode, "auto")
        val tl = tag(toCode, "en")
        val q = URLEncoder.encode(trimmed, StandardCharsets.UTF_8.name())
        val url = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=$sl&tl=$tl&dt=t&q=$q"
        val body = runCatching { httpGet(url) }.getOrNull() ?: return null
        return parseTranslation(body)
    }

    fun currencyRequest(raw: String): CurrencyRequest? {
        var s = raw.trim().lowercase(Locale.US)
        if (s.isEmpty()) return null
        s = s.replace(Regex("""\$\s*(\d+(?:\.\d+)?)"""), "$1 usd")
            .replace(Regex("""(\d+(?:\.\d+)?)\s*\$"""), "$1 usd")
            .replace(Regex("""€\s*(\d+(?:\.\d+)?)"""), "$1 eur")
            .replace(Regex("""(\d+(?:\.\d+)?)\s*€"""), "$1 eur")
            .replace(Regex("""£\s*(\d+(?:\.\d+)?)"""), "$1 gbp")
            .replace(Regex("""(\d+(?:\.\d+)?)\s*£"""), "$1 gbp")
            .replace(Regex("""¥\s*(\d+(?:\.\d+)?)"""), "$1 jpy")
            .replace(Regex("""(\d+(?:\.\d+)?)\s*¥"""), "$1 jpy")
            .replace(Regex("""₪\s*(\d+(?:\.\d+)?)"""), "$1 ils")
            .replace(Regex("""(\d+(?:\.\d+)?)\s*₪"""), "$1 ils")
            .replace(Regex("\\s+"), " ")
            .trim()
        val match = Regex(
            """^([+-]?\d+(?:\.\d+)?)\s+([a-z]+)\s+(?:to|in|as)\s+([a-z]+)$""",
        ).matchEntire(s) ?: return null
        val amount = match.groupValues[1].toDoubleOrNull() ?: return null
        val from = names[match.groupValues[2]] ?: return null
        val to = names[match.groupValues[3]] ?: return null
        if (from == to) return null
        return CurrencyRequest(raw.trim(), amount, from, to)
    }

    fun parseSpice(request: CurrencyRequest, body: String): InstantAnswer? {
        val to = Regex(""""quotecurrency"\s*:\s*"([A-Za-z]{3})"""")
            .find(body)?.groupValues?.get(1)?.uppercase(Locale.US) ?: return null
        val mid = Regex(""""mid"\s*:\s*([0-9.]+)""")
            .find(body)?.groupValues?.get(1)?.toDoubleOrNull() ?: return null
        if (!to.equals(request.to, ignoreCase = true)) return null
        return answer(request, mid, "DuckDuckGo", fromDuckDuckGo = true)
    }

    fun parseFrankfurter(request: CurrencyRequest, body: String): InstantAnswer? {
        val rate = Regex(""""${request.to}"\s*:\s*([0-9.]+)""")
            .find(body)?.groupValues?.get(1)?.toDoubleOrNull() ?: return null
        return answer(request, rate, "European Central Bank", fromDuckDuckGo = false)
    }

    fun parseRegionalFuel(query: String, place: String, body: String): InstantAnswer? {
        val currency = Regex(""""currency"\s*:\s*"([A-Z]{3})"""")
            .find(body)?.groupValues?.get(1) ?: return null
        val unit = Regex(""""unit"\s*:\s*"([a-z_]+)"""")
            .find(body)?.groupValues?.get(1) ?: "liter"
        val gasoline = jsonNumber(body, "gasoline")
        val diesel = jsonNumber(body, "diesel")
        val lpg = jsonNumber(body, "lpg")
        if (gasoline == null && diesel == null && lpg == null) return null
        val suffix = when (unit) {
            "gallon", "imperial_gallon" -> "/gal"
            else -> "/L"
        }
        val wantDiesel = query.contains("diesel", ignoreCase = true)
        val lead = if (wantDiesel) diesel ?: gasoline ?: lpg else gasoline ?: diesel ?: lpg
        val lines = buildList {
            if (!wantDiesel && gasoline != null) add("Gasoline ${money(gasoline)} $currency$suffix")
            if (diesel != null) add("Diesel ${money(diesel)} $currency$suffix")
            if (wantDiesel && gasoline != null) add("Gasoline ${money(gasoline)} $currency$suffix")
            if (lpg != null) add("LPG ${money(lpg)} $currency$suffix")
        }
        return InstantAnswer(
            heading = query,
            text = lines.joinToString(" · "),
            source = place.ifBlank { currency }.let { "$it · OpenVan" },
            url = "",
            fromDuckDuckGo = false,
            continueValue = lead?.let { money(it) },
        )
    }

    private fun jsonNumber(body: String, key: String): Double? {
        val raw = Regex(""""$key"\s*:\s*(null|[0-9.]+)""")
            .find(body)?.groupValues?.get(1) ?: return null
        return raw.toDoubleOrNull()
    }

    private fun money(value: Double): String = String.format(Locale.US, "%.2f", value)

    fun parseTranslation(body: String): String? {
        val raw = Regex("""\[\[\["((?:\\.|[^"\\])*)"""")
            .find(body)?.groupValues?.get(1) ?: return null
        val text = raw.replace("\\\"", "\"").replace("\\n", "\n").replace("\\\\", "\\").trim()
        return text.ifBlank { null }
    }

    private fun spice(request: CurrencyRequest): InstantAnswer? {
        if (!useDuckDuckGo) return null
        val amount = request.amount.toString().removeSuffix(".0")
        val url = com.madlauncher.data.DuckDuckGoApp.tag(
            "https://duckduckgo.com/js/spice/currency/$amount/${request.from.lowercase()}/${request.to.lowercase()}",
        )
        val body = runCatching { httpGet(url, referer = "https://duckduckgo.com/") }.getOrNull() ?: return null
        return parseSpice(request, body)
    }

    private fun frankfurter(request: CurrencyRequest): InstantAnswer? {
        val amount = request.amount.toString().removeSuffix(".0")
        val url = "https://api.frankfurter.app/latest?amount=$amount&from=${request.from}&to=${request.to}"
        val body = runCatching { httpGet(url) }.getOrNull() ?: return null
        return parseFrankfurter(request, body)
    }

    private fun offline(request: CurrencyRequest): InstantAnswer? {
        val json = offlineRatesJson?.takeIf { it.isNotBlank() } ?: return null
        val from = rate(json, request.from) ?: return null
        val to = rate(json, request.to) ?: return null
        if (from == 0.0) return null
        val value = request.amount / from * to
        return answer(request, value, "Offline rates", fromDuckDuckGo = false)
    }

    private fun rate(json: String, code: String): Double? {
        if (code == "USD") return 1.0
        return Regex(""""$code"\s*:\s*([0-9.]+)""")
            .find(json)?.groupValues?.get(1)?.toDoubleOrNull()
    }

    private fun answer(
        request: CurrencyRequest,
        value: Double,
        source: String,
        fromDuckDuckGo: Boolean,
    ): InstantAnswer {
        val number = String.format(Locale.US, "%.2f", value)
        return InstantAnswer(
            heading = request.query,
            text = "$number ${request.to}",
            source = source,
            url = "",
            fromDuckDuckGo = fromDuckDuckGo,
            continueValue = number,
        )
    }

    private fun isFuelQuery(raw: String): Boolean {
        val s = raw.trim().lowercase(Locale.US).replace(Regex("\\s+"), " ")
        return s == "gas" || s == "gas price" || s == "gas prices" ||
            s == "petrol" || s == "petrol price" || s == "diesel" ||
            s == "diesel price" || s == "fuel" || s == "fuel price" ||
            s == "fuel prices"
    }

    private fun tag(code: String?, fallback: String): String {
        val raw = code?.trim()?.lowercase(Locale.US)?.substringBefore('-').orEmpty()
        return if (raw.matches(Regex("[a-z]{2,3}"))) raw else fallback
    }

    private fun httpGet(url: String, referer: String? = null): String {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 5000
            readTimeout = 5000
            requestMethod = "GET"
            setRequestProperty(
                "User-Agent",
                "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/120.0.0.0 Mobile Safari/537.36",
            )
            if (referer != null) setRequestProperty("Referer", referer)
        }
        return try {
            conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }
}

data class BoxedTranslation(
    val code: String,
    val label: String,
    val text: String,
)

data class BoxedConversion(
    val code: String,
    val label: String,
    val text: String,
)

data class CurrencyRequest(
    val query: String,
    val amount: Double,
    val from: String,
    val to: String,
)
