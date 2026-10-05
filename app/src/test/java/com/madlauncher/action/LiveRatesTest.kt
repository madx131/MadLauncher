package com.madlauncher.action

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LiveRatesTest {
    @Test
    fun currencyQueryReadsAmountAndCodes() {
        val request = LiveRates.currencyRequest("100 usd to eur")!!
        assertEquals(100.0, request.amount, 0.001)
        assertEquals("USD", request.from)
        assertEquals("EUR", request.to)
        assertEquals(LiveRates.Kind.CURRENCY, LiveRates.classify("\$50 to euros"))
        assertNull(LiveRates.currencyRequest("2+2"))
    }

    @Test
    fun spiceAmountStaysInTheBox() {
        val request = LiveRates.currencyRequest("100 usd to eur")!!
        val body = """ddg_spice_currency({"from":"USD","amount":100.0,"to":[{"quotecurrency":"EUR","mid":88.89}]});"""
        val hit = LiveRates.parseSpice(request, body)!!
        assertEquals("88.89 EUR", hit.text)
        assertEquals("88.89", hit.continueValue)
        assertEquals("", hit.url)
        assertEquals("100 usd to eur", hit.heading)
    }

    @Test
    fun fuelPricesUseTheLocalCurrency() {
        val body = """
            {"data":{"IL":{"currency":"ILS","unit":"liter","prices":{"gasoline":8.25,"diesel":7.8873,"lpg":null}}}}
        """.trimIndent()
        val hit = LiveRates.parseRegionalFuel("gas price", "Tel Aviv, Israel", body)!!
        assertEquals("8.25", hit.continueValue)
        assertEquals("Gasoline 8.25 ILS/L · Diesel 7.89 ILS/L", hit.text)
        assertEquals("Tel Aviv, Israel · OpenVan", hit.source)
        assertEquals("", hit.url)
        val diesel = LiveRates.parseRegionalFuel("diesel", "Tel Aviv, Israel", body)!!
        assertEquals("7.89", diesel.continueValue)
    }

    @Test
    fun translationTextIsTheFirstString() {
        val body = """[[["Hola","hello",null,null,10]],null,"en"]"""
        assertEquals("Hola", LiveRates.parseTranslation(body))
    }

    @Test
    fun offlineRatesConvertWithoutANetwork() {
        LiveRates.offlineRatesJson = """{"base":"USD","rates":{"EUR":0.8851}}"""
        try {
            val request = LiveRates.currencyRequest("100 usd to eur")!!
            val hit = LiveRates.parseFrankfurter(
                request,
                """{"amount":100.0,"base":"USD","rates":{"EUR":88.51}}""",
            )!!
            assertEquals("88.51", hit.continueValue)
        } finally {
            LiveRates.offlineRatesJson = null
        }
    }
}
