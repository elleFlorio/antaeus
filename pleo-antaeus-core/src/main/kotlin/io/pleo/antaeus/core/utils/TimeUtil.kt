package io.pleo.antaeus.core.utils

import java.text.DecimalFormat

object TimeUtil {
    private val doubleDigitFormat = DecimalFormat("00")

    fun generateRandomHour(): String {
        val hour = (0..23).shuffled().first()
        return doubleDigitFormat.format(hour)
    }

    fun generateRandomMinutes(): String {
        val minutes = (0..59).shuffled().first()
        return doubleDigitFormat.format(minutes)
    }
}