package io.pleo.antaeus.core.utils

import java.text.DecimalFormat
import kotlin.random.Random

object TimeUtil {
    private val doubleDigitFormat = DecimalFormat("00")
    private val doubleDigitNumbers = arrayOf("00", "01", "02", "03", "04", "05", "06", "07", "08", "09")
    private val validDays = doubleDigitNumbers + (10..31).map { it.toString() }
    private val validHours = doubleDigitNumbers + (10..23).map { it.toString() }
    private val validMinutes = doubleDigitNumbers + (10..59).map { it.toString() }

    fun generateRandomHour(from: Int = 0, until: Int = 23): String {
        val hour = Random.nextInt(from, until)
        return doubleDigitFormat.format(hour)
    }

    fun generateRandomMinutes(from: Int = 0, until: Int = 59): String {
        val minutes = Random.nextInt(from, until)
        return doubleDigitFormat.format(minutes)
    }

    fun generateRandomDelayMillis(): Long = Random.nextLong(1000, 3000)

    fun isValidStringDay(day: String) = validDays.contains(day)

    fun isValidStringHour(hour: String) = validHours.contains(hour)

    fun isValidStringMinute(minute: String) = validMinutes.contains(minute)
}