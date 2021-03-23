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

    fun fetch(id: Int): Invoice {
        return dal.fetchInvoice(id) ?: throw InvoiceNotFoundException(id)
    }

    fun fetch(status: InvoiceStatus): List<Invoice> {
        return dal.fetchInvoicesByStatus(status)
    }

    fun markInvoiceAsPaid(id: Int): Int {
        return dal.updateInvoice(id, InvoiceStatus.PAID, "charged-successfully")
    }

    fun markInvoiceAsFailed(id: Int, event: String): Int {
        return dal.updateInvoice(id, InvoiceStatus.FAILED, event)
    }

    fun traceRetriableError(id: Int, event: String): Int {
        return dal.createInvoiceEvent(id, InvoiceStatus.PENDING, event)
    }
}
