/*
    Implements endpoints related to invoices.
 */

package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.InvoiceNotFoundException
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus

class InvoiceService(private val dal: AntaeusDal) {
    fun fetchAll(): List<Invoice> {
       return dal.fetchInvoices()
    }

    fun fetchAllWithStatus(status: InvoiceStatus): List<Invoice> {
        return dal.fetchInvoices(status)
    }

    fun fetch(id: Int): Invoice {
        return dal.fetchInvoice(id) ?: throw InvoiceNotFoundException(id)
    }

    fun markAsPaid(paid: Invoice): Invoice {
        val updated = paid.copy(status = InvoiceStatus.PAID)
        return dal.updateInvoice(updated) ?: throw InvoiceNotFoundException(paid.id)
    }

    fun markAsFailed(paid: Invoice): Invoice {
        val updated = paid.copy(status = InvoiceStatus.FAILED)
        return dal.updateInvoice(updated) ?: throw InvoiceNotFoundException(paid.id)
    }
}
