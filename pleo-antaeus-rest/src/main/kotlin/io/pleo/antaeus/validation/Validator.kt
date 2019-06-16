package io.pleo.antaeus.validation

import io.pleo.antaeus.core.utils.TimeUtil
import java.lang.NumberFormatException
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

object Validator {

    fun isValidInvoiceId(input: String): Boolean {
        return try {
            input.toInt()
            true
        } catch (nfe: NumberFormatException) {
            false
        }
    }

    fun isValidPeriodicScheduling(input: String): Boolean {
        return try {
            val (day, hour, minute) = input.split(":")
            TimeUtil.isValidStringDay(day) && TimeUtil.isValidStringHour(hour) && TimeUtil.isValidStringMinute(minute)
        } catch (e: Exception) {
            false
        }
    }
}