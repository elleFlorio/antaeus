package io.pleo.antaeus.core.services.billing


import io.pleo.antaeus.core.exceptions.*
import io.pleo.antaeus.core.external.CurrencyConversionProvider
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.core.notification.Message
import io.pleo.antaeus.core.notification.NotificationService
import io.pleo.antaeus.core.notification.OutcomeType
import io.pleo.antaeus.core.scheduler.Scheduler
import io.pleo.antaeus.core.services.CustomerService
import io.pleo.antaeus.core.services.InvoiceService
import io.pleo.antaeus.core.utils.TimeUtil
import io.pleo.antaeus.core.utils.TimeUtil.generateRandomHour
import io.pleo.antaeus.core.utils.TimeUtil.generateRandomMinutes
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import mu.KotlinLogging
import kotlinx.coroutines.*

private const val BILLING_DAY = "1"
private const val NUM_MAX_RETRY = 3
private val logger = KotlinLogging.logger {}

class BillingService(
        private val paymentProvider: PaymentProvider,
        private val invoiceService: InvoiceService,
        private val customerService: CustomerService,
        private val currencyConversionProvider: CurrencyConversionProvider,
        private val notificationService: NotificationService,
        private val scheduler: Scheduler
) {
    fun requestPayment(invoiceId: Int): OutcomeType {
        return try {
            val invoice = invoiceService.fetch(invoiceId)
            if (invoice.status == InvoiceStatus.PAID) { OutcomeType.SUCCESS }
            else {
                val paymentOutcome = tryPaymentRequest(invoice)
                if (paymentOutcome.result) {
                    invoiceService.markAsPaid(invoice)
                    notificationService.notifySuccess(Message(invoice, paymentOutcome.outcome))
                } else {
                    invoiceService.markAsFailed(invoice)
                    notificationService.notifyFailure(Message(invoice, paymentOutcome.outcome))
                }

                paymentOutcome.outcome
            }
        } catch (infe: InvoiceNotFoundException) {
            logger.debug(infe) { "Cannot find invoice" }
            OutcomeType.FAILURE
        }
    }

    fun isPeriodicBillingActive() = scheduler.isTaskActive()

    fun cancelPeriodicBilling() {
        scheduler.stopActiveTask()
        logger.info { "periodic billing stopped" }
    }

    fun startPeriodicBilling() {
        val hour = generateRandomHour(until = 22)
        val minutes = generateRandomMinutes()
        setPeriodicBilling(BILLING_DAY, hour, minutes)
    }

    fun setPeriodicBilling(day: String, hour: String, minutes: String) {
        if (scheduler.isTaskActive()) scheduler.stopActiveTask()

        scheduler.scheduleTask(paymentTask, day, hour, minutes)
        logger.info { "periodic billing scheduled: $day of month at $hour:$minutes" }
    }

    private val paymentTask: () -> Unit = {
        logger.info { "Starting periodic billing task..." }

        val pending = invoiceService.fetchAllWithStatus(InvoiceStatus.PENDING)
        logger.info {"Number of pending invoices: ${pending.size}"}

        val (paid, failed) = pending.map { tryPaymentRequest(it) }.partition { it.result }

        paid.map { paymentOutcome -> invoiceService.markAsPaid(paymentOutcome.invoice); paymentOutcome }
                .map { Message(it.invoice, it.outcome) }
                .map { notificationService.notifySuccess(it) }

        failed.map { paymentOutcome -> invoiceService.markAsFailed(paymentOutcome.invoice); paymentOutcome }
                .map { Message(it.invoice, it.outcome) }
                .map { notificationService.notifyFailure(it) }

        logger.info { "Periodic billing executed: paid:${paid.size}, failed:${failed.size}" }
    }

    fun tryPaymentRequest(invoice: Invoice, tentative: Int = 0): PaymentRequestOutcome {
        val failure = PaymentRequestOutcome(invoice, false, OutcomeType.FAILURE)

        if (tentative < NUM_MAX_RETRY) {
            return try {
                val paid = paymentProvider.charge(invoice)
                if (paid) PaymentRequestOutcome(invoice, true, OutcomeType.SUCCESS) else failure
            } catch (cnfe: CustomerNotFoundException) {
                // Cannot handle this case, set proper outcome and delegate to operator/other service
                logger.warn(cnfe) { "Cannot find customer" }
                PaymentRequestOutcome(invoice, false, OutcomeType.CUSTOMER_NOT_FOUND)

            } catch (cme: CurrencyMismatchException) {
                // Convert the amount to customer currency and try another payment request
                logger.warn(cme) { "Currency doesn't match, trying conversion" }
                val convertedInvoice = convertCurrency(invoice)
                convertedInvoice?.let { tryPaymentRequest(convertedInvoice) } ?: failure

            } catch (ne: NetworkException) {
                // If there is a network error, I should retry NUM_MAX_RETRY times
                logger.warn(ne) { "Network error, tentavive ${tentative + 1}" }
                runBlocking {
                        retryPaymentRequest(invoice, tentative)
                }
            }
        }

        return failure
    }

    private fun convertCurrency(invoice: Invoice, tentative: Int = 0): Invoice? {
        if (tentative < NUM_MAX_RETRY) {
            return try {
                val customer = customerService.fetch(invoice.customerId)
                val convertedAmount = currencyConversionProvider.convertCurrency(invoice.amount, customer.currency)
                invoice.copy(amount = convertedAmount)

            } catch (cnfe: CustomerNotFoundException) {
                logger.warn(cnfe) { "Cannot find customer" }
                null
            } catch (cunfe: CurrencyNotFoundException) {
                logger.warn(cunfe) { "Currency not found" }
                null
            } catch (ne: NetworkException) {
                logger.warn(ne) { "Network error, tentavive ${tentative + 1}" }
                runBlocking {
                    retryConvertCurrency(invoice, tentative)
                }
            }
        }

        return null
    }

    private suspend fun retryConvertCurrency(invoice: Invoice, tentative: Int = 0): Invoice? {
        delay(TimeUtil.generateRandomDelayMillis())
        return convertCurrency(invoice, tentative + 1)
    }

    private suspend fun retryPaymentRequest(invoice: Invoice, tentative: Int = 0): PaymentRequestOutcome {
        delay(TimeUtil.generateRandomDelayMillis())
        return tryPaymentRequest(invoice, tentative + 1)
    }
}