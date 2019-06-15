package io.pleo.antaeus.core.services

import io.mockk.every
import io.mockk.mockk
import io.pleo.antaeus.core.exceptions.InvoiceNotFoundException
import io.pleo.antaeus.core.external.CurrencyConversionProvider
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.core.notification.Message
import io.pleo.antaeus.core.notification.NotificationService
import io.pleo.antaeus.core.notification.OutcomeType
import io.pleo.antaeus.core.scheduler.Scheduler
import io.pleo.antaeus.core.services.billing.BillingService
import io.pleo.antaeus.core.services.billing.PaymentRequestOutcome
import io.pleo.antaeus.models.*
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.math.BigDecimal

private val MONEY = Money(BigDecimal(100), Currency.DKK)
private val INVOICE_SUCCESS = Invoice(200, 200, MONEY, InvoiceStatus.PENDING)
private val INVOICE_FAILURE = INVOICE_SUCCESS.copy(id = 500, customerId = 500)
private val CUSTOMER =  Customer(200, Currency.DKK)

class BillingServiceTest {

    private val paymentProvider = mockk<PaymentProvider> {
        every { charge(INVOICE_SUCCESS) } returns true
        every { charge(INVOICE_FAILURE) } returns false
    }

    private val invoiceService = mockk<InvoiceService> {
        every { fetch(200) } returns INVOICE_SUCCESS
        every { fetch(500) } throws InvoiceNotFoundException(500)
    }

    private val customerService = mockk<CustomerService> {
        every { fetch(200) } returns CUSTOMER
    }

    private val currencyConversionProvider = mockk<CurrencyConversionProvider> {
        every { convertCurrency(MONEY, Currency.EUR) }
    }

    private val notificationService = mockk<NotificationService> {
        every { notifySuccess(Message(INVOICE_SUCCESS, OutcomeType.SUCCESS)) }
        every { notifySuccess(Message(INVOICE_FAILURE, OutcomeType.FAILURE)) }
    }

    private val scheduler = mockk<Scheduler> {
        every { isTaskActive() } returns true
    }

    private val billingService = BillingService(
            paymentProvider = paymentProvider,
            invoiceService = invoiceService,
            customerService = customerService,
            currencyConversionProvider = currencyConversionProvider,
            notificationService = notificationService,
            scheduler = scheduler
    )

    @Test
    fun `will return a success outcome if the billing is done`() {
        val outcome = billingService.tryPaymentRequest(INVOICE_SUCCESS)
        Assertions.assertEquals(PaymentRequestOutcome(INVOICE_SUCCESS, true, OutcomeType.SUCCESS), outcome)
    }

    @Test
    fun `will return a failure outcome if the billing is failed`() {
        val outcome = billingService.tryPaymentRequest(INVOICE_FAILURE)
        Assertions.assertEquals(PaymentRequestOutcome(INVOICE_FAILURE, false, OutcomeType.FAILURE), outcome)
    }
}