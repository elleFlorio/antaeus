package io.pleo.antaeus.core.services


import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.core.scheduler.Scheduler
import io.pleo.antaeus.core.utils.TimeUtil.generateRandomHour
import io.pleo.antaeus.core.utils.TimeUtil.generateRandomMinutes
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus

private const val NUM_MAX_RETRY = 3

class BillingService(
        private val paymentProvider: PaymentProvider,
        private val invoiceService: InvoiceService,
        private val customerService: CustomerService, //TODO use for currency conversion
        scheduler: Scheduler
) {

    private val paymentTask: () -> Unit = {
        val pending = invoiceService.fetchAllWithStatus(InvoiceStatus.PENDING)
        val (paid, failed) = pending.map { tryPaymentRequest(it) }.partition { it.second }
        paid.map { invoiceService.markAsPaid(it.first) }
        failed.map { invoiceService.markAsFailed(it.first) }
        //TODO integrate notification service to send paid/failed notifications
    }

    private fun tryPaymentRequest(invoice: Invoice, tentative: Int = 0): Pair<Invoice, Boolean> {
        var result = false
        if (tentative < NUM_MAX_RETRY) {
            try {
                result = paymentProvider.charge(invoice)
            } catch (cnfe: CustomerNotFoundException) {
                result = false
            } catch (cme: CurrencyMismatchException) {

                /* TODO integrate external currency conversion service
                *   1. check customer currency
                *   2. convert currency using external service
                *   3. create invoice with new currency
                *   4. charge payment
                * */

            } catch (ne: NetworkException) {
                // If there is a network error, I should retry NUM_MAX_RETRY times
                result = tryPaymentRequest(invoice, tentative + 1).second
            }
        }

        return Pair(invoice, result)
    }

    init {
        val hour = generateRandomHour()
        val minutes = generateRandomMinutes()
        scheduler.scheduleTask(paymentTask, hour, minutes)
    }

}