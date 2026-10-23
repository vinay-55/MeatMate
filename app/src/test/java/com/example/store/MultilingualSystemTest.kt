package com.example.store

import com.example.store.LocaleHelper.LANG_ENGLISH
import com.example.store.LocaleHelper.LANG_HINDI
import com.example.store.LocaleHelper.LANG_KANNADA
import com.example.store.LocaleHelper.LANG_KANNADA_LATN
import com.example.store.LocaleHelper.LANG_TAMIL
import com.example.store.LocaleHelper.LANG_TELUGU
import com.example.store.LocaleHelper.LANG_URDU
import org.junit.Assert.*
import org.junit.Test
import java.time.DayOfWeek
import java.util.Locale

class MultilingualSystemTest {

    @Test
    fun testAllSevenLanguagesConfigured() {
        val languages = LocaleHelper.SUPPORTED_LANGUAGES
        assertEquals("There must be exactly 7 supported language options", 7, languages.size)

        val codes = languages.map { it.code }
        assertTrue(codes.contains(LANG_KANNADA))
        assertTrue(codes.contains(LANG_ENGLISH))
        assertTrue(codes.contains(LANG_HINDI))
        assertTrue(codes.contains(LANG_TAMIL))
        assertTrue(codes.contains(LANG_TELUGU))
        assertTrue(codes.contains(LANG_URDU))
        assertTrue(codes.contains(LANG_KANNADA_LATN))
    }

    @Test
    fun testLocaleResolution() {
        val knLocale = LocaleHelper.getLocale(LANG_KANNADA)
        assertEquals("kn", knLocale.language)

        val enLocale = LocaleHelper.getLocale(LANG_ENGLISH)
        assertEquals("en", enLocale.language)

        val hiLocale = LocaleHelper.getLocale(LANG_HINDI)
        assertEquals("hi", hiLocale.language)

        val taLocale = LocaleHelper.getLocale(LANG_TAMIL)
        assertEquals("ta", taLocale.language)

        val teLocale = LocaleHelper.getLocale(LANG_TELUGU)
        assertEquals("te", teLocale.language)

        val urLocale = LocaleHelper.getLocale(LANG_URDU)
        assertEquals("ur", urLocale.language)

        val latnLocale = LocaleHelper.getLocale(LANG_KANNADA_LATN)
        assertEquals("kn-Latn", latnLocale.toLanguageTag())
    }

    @Test
    fun testKannadaTransliterationDayNames() {
        assertEquals("Somavara", LocaleHelper.formatDayOfWeek(LANG_KANNADA_LATN, DayOfWeek.MONDAY))
        assertEquals("Mangalavara", LocaleHelper.formatDayOfWeek(LANG_KANNADA_LATN, DayOfWeek.TUESDAY))
        assertEquals("Budhavara", LocaleHelper.formatDayOfWeek(LANG_KANNADA_LATN, DayOfWeek.WEDNESDAY))
        assertEquals("Guruvara", LocaleHelper.formatDayOfWeek(LANG_KANNADA_LATN, DayOfWeek.THURSDAY))
        assertEquals("Shukravara", LocaleHelper.formatDayOfWeek(LANG_KANNADA_LATN, DayOfWeek.FRIDAY))
        assertEquals("Shanivara", LocaleHelper.formatDayOfWeek(LANG_KANNADA_LATN, DayOfWeek.SATURDAY))
        assertEquals("Bhanuvara", LocaleHelper.formatDayOfWeek(LANG_KANNADA_LATN, DayOfWeek.SUNDAY))
    }

    @Test
    fun testNumericFormattingAlwaysUsesWesternDigits() {
        val price = 150.50
        val weight = 12.50
        val total = 1250.00
        val serialNumber = 123
        val dateString = String.format(Locale.ENGLISH, "%04d-%02d-%02d", 2026, 10, 17)

        val formattedPrice = String.format(Locale.ENGLISH, "%.2f", price)
        val formattedWeight = String.format(Locale.ENGLISH, "%.2f", weight)
        val formattedTotal = String.format(Locale.ENGLISH, "%.2f", total)
        val formattedSerial = serialNumber.toString()

        assertEquals("150.50", formattedPrice)
        assertEquals("12.50", formattedWeight)
        assertEquals("1250.00", formattedTotal)
        assertEquals("123", formattedSerial)
        assertEquals("2026-10-17", dateString)

        val digitRegex = Regex("^[0-9.,:-]+$")
        assertTrue(formattedPrice.matches(digitRegex))
        assertTrue(formattedWeight.matches(digitRegex))
        assertTrue(formattedTotal.matches(digitRegex))
        assertTrue(formattedSerial.matches(digitRegex))
        assertTrue(dateString.matches(digitRegex))

        // Ensure no Arabic-Indic (٠١٢٣٤٥٦٧٨٩) or Devanagari (०१२३४५६७८९) digits leak into numbers
        val nonWesternRegex = Regex("[\\u0660-\\u0669\\u06F0-\\u06F9\\u0966-\\u096F\\u0C66-\\u0C6F\\u0CE6-\\u0CEF]")
        assertFalse(formattedPrice.contains(nonWesternRegex))
        assertFalse(formattedWeight.contains(nonWesternRegex))
        assertFalse(formattedTotal.contains(nonWesternRegex))
        assertFalse(dateString.contains(nonWesternRegex))
    }
}
