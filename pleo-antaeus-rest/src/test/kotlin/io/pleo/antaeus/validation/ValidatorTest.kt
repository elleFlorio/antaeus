package io.pleo.antaeus.validation

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class ValidatorTest {

    @Test
    fun `will accept Int invoice id`() {
        Assertions.assertTrue(Validator.isValidInvoiceId("1"))
    }

    @Test
    fun `will reject non Int invoice id`() {
        Assertions.assertFalse(Validator.isValidInvoiceId("a"))
    }

    @Test
    fun `will accept valid date for periodic billing`() {
        Assertions.assertTrue(Validator.isValidPeriodicScheduling("01:00:00"))
    }

    @Test
    fun `will reject not valid date for periodic billing`() {
        Assertions.assertFalse(Validator.isValidPeriodicScheduling("32:00:00"))
        Assertions.assertFalse(Validator.isValidPeriodicScheduling("32:00"))
        Assertions.assertFalse(Validator.isValidPeriodicScheduling("a"))
    }
}