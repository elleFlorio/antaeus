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
import io.pleo.antaeus.core.utils.TimeUtil.generateRandomHour
import io.pleo.antaeus.core.utils.TimeUtil.generateRandomMinutes
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import mu.KotlinLogging

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
        val hour = generateRandomHour()
        val minutes = generateRandomMinutes()
        scheduler.scheduleTask(paymentTask, hour, minutes)
        logger.info { "periodic billing started: 1 of month at $hour:$minutes" }
    }

    private val paymentTask: () -> Unit = {
        val pending = invoiceService.fetchAllWithStatus(InvoiceStatus.PENDING)
        val (paid, failed) = pending.map { tryPaymentRequest(it) }.partition { it.result }

        paid.map { paymentOutcome -> invoiceService.markAsPaid(paymentOutcome.invoice); paymentOutcome }
                .map { Message(it.invoice, it.outcome) }
                .map { notificationService.notifySuccess(it) }

        failed.map { paymentOutcome -> invoiceService.markAsFailed(paymentOutcome.invoice); paymentOutcome }
                .map { Message(it.invoice, it.outcome) }
                .map { notificationService.notifyFailure(it) }

        logger.debug { "Periodic billing executed: paid:${paid.size}, failed:${failed.size}" }
    }

    fun tryPaymentRequest(invoice: Invoice, tentative: Int = 0): PaymentRequestOutcome {
        val failure = PaymentRequestOutcome(invoice, false, OutcomeType.FAILURE)

        if (tentative < NUM_MAX_RETRY) {
            return try {
                val paid = paymentProvider.charge(invoice)
                if (paid) PaymentRequestOutcome(invoice, true, OutcomeType.SUCCESS) else failure
            } catch (cnfe: CustomerNotFoundException) {
                // Cannot handle this case, set proper outcome and delegate to operator/other service
                logger.debug(cnfe) { "Cannot find customer" }
                PaymentRequestOutcome(invoice, false, OutcomeType.CUSTOMER_NOT_FOUND)

            } catch (cme: CurrencyMismatchException) {
                // Convert the amount to customer currency and try another payment request
                logger.debug(cme) { "Currency doesn't match, trying conversion" }
                val convertedInvoice = convertCurrency(invoice)
                convertedInvoice?.let { tryPaymentRequest(convertedInvoice) } ?: failure

            } catch (ne: NetworkException) {
                // If there is a network error, I should retry NUM_MAX_RETRY times
                logger.debug(ne) { "Network error, tentavive ${tentative + 1}" }
                tryPaymentRequest(invoice, tentative + 1)
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
                logger.debug(cnfe) { "Cannot find customer" }
                null
            } catch (cunfe: CurrencyNotFoundException) {
                logger.debug(cunfe) { "Currency not found" }
                null
            } catch (ne: NetworkException) {
                logger.debug(ne) { "Network error, tentavive ${tentative + 1}" }
                convertCurrency(invoice, tentative + 1)
            }
        }

        return null
    }
}