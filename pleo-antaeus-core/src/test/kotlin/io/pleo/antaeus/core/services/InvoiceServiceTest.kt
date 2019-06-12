package io.pleo.antaeus.core.services

import io.mockk.every
import io.mockk.mockk
import io.pleo.antaeus.core.exceptions.InvoiceNotFoundException
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Money
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal

class InvoiceServiceTest {
    private val paid = Invoice(404, 404, Money(BigDecimal(0), Currency.USD), InvoiceStatus.PAID)
    private val failed = paid.copy(status = InvoiceStatus.FAILED)
    private val dal = mockk<AntaeusDal> {
        every { fetchInvoice(404) } returns null
        every { updateInvoice(paid) } returns null
        every { updateInvoice(failed) } returns null
    }

    private val invoiceService = InvoiceService(dal = dal)

    @Test
    fun `will throw if customer is not found`() {
        assertThrows<InvoiceNotFoundException> {
            invoiceService.fetch(404)
        }
    }

    @Test
    fun `will throw if invoice to mark as paid is not found`() {
        assertThrows<InvoiceNotFoundException> {
            invoiceService.markAsPaid(paid)
        }
    }

    @Test
    fun `will throw if invoice to mark as failed is not found`() {
        assertThrows<InvoiceNotFoundException> {
            invoiceService.markAsFailed(failed)
        }
    }
}