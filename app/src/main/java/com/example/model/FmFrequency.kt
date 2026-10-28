package com.example.model

import java.util.Locale
import kotlin.math.roundToInt

object FmFrequency {
    const val MIN_TENTHS = 875  // 87.5 MHz
    const val MAX_TENTHS = 1080 // 108.0 MHz
    const val DEFAULT_TENTHS = 939 // 93.9 MHz

    fun clamp(freqTenths: Int): Int = freqTenths.coerceIn(MIN_TENTHS, MAX_TENTHS)

    fun wrap(freqTenths: Int): Int {
        val span = MAX_TENTHS - MIN_TENTHS + 1
        val normalized = ((freqTenths - MIN_TENTHS) % span + span) % span
        return MIN_TENTHS + normalized
    }

    fun formatMhz(freqTenths: Int): String {
        val clamped = clamp(freqTenths)
        return String.format(Locale.US, "%.1f", clamped / 10.0)
    }

    fun parseUserFrequencyTenths(query: String): Int? {
        val cleaned = query.trim()
            .lowercase(Locale.US)
            .removeSuffix("mhz")
            .removeSuffix("fm")
            .trim()
            .replace(',', '.')
        val value = cleaned.toDoubleOrNull() ?: return null
        val tenths = (value * 10.0).roundToInt()
        return if (tenths in MIN_TENTHS..MAX_TENTHS) tenths else null
    }

    /**
     * Extracts a real terrestrial FM broadcast frequency (87.5 - 108.0 MHz)
     * from a station's name or tags if present (e.g. "Los 40 93.9 FM", "Kiss 102.7").
     */
    private val fmRegex = Regex("""(?<!\d)(8[7-9]\.[5-9]|8[8-9]\.[0-4]|9\d\.\d|10[0-7]\.\d|108\.0)(?!\d)""")

    fun extractFromText(text: String): Int? {
        val normalized = text.replace(',', '.')
        val match = fmRegex.find(normalized) ?: return null
        val parsed = match.value.toDoubleOrNull() ?: return null
        val tenths = (parsed * 10.0).roundToInt()
        return if (tenths in MIN_TENTHS..MAX_TENTHS) tenths else null
    }
}
