package io.pleo.antaeus.core.utils

import java.text.DecimalFormat

object TimeUtil {

    fun generateRandomHour(): String {
        val hour = (0..23).shuffled().first()
        val doubleDigitFormat = DecimalFormat("00")
        return doubleDigitFormat.format(hour)
    }

    fun generateRandomMinutes(): String = (0..59).shuffled().first().toString()
}