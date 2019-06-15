package io.pleo.antaeus.core.utils


import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class TimeUtilTest {

    private val doubleDigitNumbers = arrayOf("00", "01", "02", "03", "04", "05", "06", "07", "08", "09")
    private val validHours = doubleDigitNumbers + (10..23).map { it.toString() }
    private val validMinutes = doubleDigitNumbers + (10..59).map { it.toString() }

    @Test
    fun `will generate correct hours`() {
        //Probably not the best way, but reasonable...
        for (i in 0..1000) {
            Assertions.assertTrue(validHours.contains(TimeUtil.generateRandomHour()))
        }
    }

    @Test
    fun `will generate correct minutes`() {
        //Probably not the best way, but reasonable...
        for (i in 0..1000) {
            Assertions.assertTrue(validMinutes.contains(TimeUtil.generateRandomMinutes()))
        }
    }


}