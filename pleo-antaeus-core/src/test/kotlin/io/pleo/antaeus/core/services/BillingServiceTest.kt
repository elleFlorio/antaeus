package io.pleo.antaeus.core.services

import io.mockk.every
import io.mockk.mockk
import io.pleo.antaeus.core.exceptions.*
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

// Money
private val MONEY = Money(BigDecimal(100), Currency.DKK)
private val MONEY_CURRENCY_NOT_FOUND = Money(BigDecimal(404), Currency.DKK)
private val MONEY_NETWORK_ERROR = Money(BigDecimal(504), Currency.DKK)

// Invoice
private val INVOICE_SUCCESS = Invoice(200, 200, MONEY, InvoiceStatus.PENDING)
private val INVOICE_FAILURE = INVOICE_SUCCESS.copy(id = 500, customerId = 500)
private val INVOICE_CUSTOMER_NOT_FOUND = INVOICE_SUCCESS.copy(id = 200, customerId = 404)
private val INVOICE_CURRENCY_MISMATCH = INVOICE_SUCCESS.copy(id = 303, customerId = 303)
private val INVOICE_NETWORK_ERROR = INVOICE_SUCCESS.copy(id = 504)

// Customer
private val CUSTOMER_DKK =  Customer(200, Currency.DKK)
private val CUSTOMER_EUR =  Customer(303, Currency.EUR)

class BillingServiceTest {

    private val paymentProvider = mockk<PaymentProvider> {
        every { charge(INVOICE_SUCCESS) } returns true
        every { charge(INVOICE_FAILURE) } returns false
        every { charge(INVOICE_CUSTOMER_NOT_FOUND) } throws CustomerNotFoundException(404)
        every { charge(INVOICE_CURRENCY_MISMATCH) } throws CurrencyMismatchException(303, 303)
        every { charge(INVOICE_CURRENCY_MISMATCH.copy(amount = MONEY.copy(currency = Currency.EUR)))} returns true
        every { charge(INVOICE_CURRENCY_MISMATCH.copy(amount = MONEY_CURRENCY_NOT_FOUND))} throws CurrencyMismatchException(303, 303)
        every { charge(INVOICE_CURRENCY_MISMATCH.copy(amount = MONEY_NETWORK_ERROR))} throws CurrencyMismatchException(303, 303)
        every {charge(INVOICE_NETWORK_ERROR)} throws NetworkException()
    }

    private val invoiceService = mockk<InvoiceService> {
        every { fetch(200) } returns INVOICE_SUCCESS
        every { fetch(500) } throws InvoiceNotFoundException(500)
    }

    private val customerService = mockk<CustomerService> {
        every { fetch(200) } returns CUSTOMER_DKK
        every { fetch(303) } returns CUSTOMER_EUR
        every { fetch(404) } throws CustomerNotFoundException(404)
    }

    private val currencyConversionProvider = mockk<CurrencyConversionProvider> {
        every { convertCurrency(MONEY, Currency.EUR) } returns MONEY.copy(currency = Currency.EUR)
        every { convertCurrency(MONEY_CURRENCY_NOT_FOUND, Currency.EUR) } throws CurrencyNotFoundException(Currency.EUR)
        every { convertCurrency(MONEY_NETWORK_ERROR, Currency.EUR) } throws NetworkException()
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

    @Test
    fun `will return a failure outcome if the customer is not found`() {
        val outcome = billingService.tryPaymentRequest(INVOICE_CUSTOMER_NOT_FOUND)
        Assertions.assertEquals(PaymentRequestOutcome(INVOICE_CUSTOMER_NOT_FOUND, false, OutcomeType.CUSTOMER_NOT_FOUND), outcome)
    }

    @Test
    fun `will convert money and return a success outcome if the money need to be converted`() {
        val outcome = billingService.tryPaymentRequest(INVOICE_CURRENCY_MISMATCH)
        val convertedMoney = MONEY.copy(currency = Currency.EUR)
        Assertions.assertEquals(PaymentRequestOutcome(INVOICE_CURRENCY_MISMATCH.copy(amount = convertedMoney), true, OutcomeType.SUCCESS), outcome)
    }

    @Test
    fun `will convert money and return a failure outcome if there is a conversion error`() {
        val invoice = INVOICE_CURRENCY_MISMATCH.copy(amount = MONEY_CURRENCY_NOT_FOUND)
        val outcome = billingService.tryPaymentRequest(invoice)
        Assertions.assertEquals(PaymentRequestOutcome(invoice, false, OutcomeType.FAILURE), outcome)
    }

    @Test
    fun `will convert money and return a failure outcome after too many tentative requests with network error`() {
        val invoice = INVOICE_CURRENCY_MISMATCH.copy(amount = MONEY_NETWORK_ERROR)
        val outcome = billingService.tryPaymentRequest(invoice)
        Assertions.assertEquals(PaymentRequestOutcome(invoice, false, OutcomeType.FAILURE), outcome)
    }

    @Test
    fun `will return a failure after too many tentative requests with network error`() {
        val outcome = billingService.tryPaymentRequest(INVOICE_NETWORK_ERROR)
        Assertions.assertEquals(PaymentRequestOutcome(INVOICE_NETWORK_ERROR, false, OutcomeType.FAILURE), outcome)
    }
}