/*
    Implements endpoints related to invoices.
 */

package io.pleo.antaeus.core.services

import arrow.core.Either
import io.pleo.antaeus.core.exceptions.*
import io.pleo.antaeus.core.external.SuccessfullyCharged
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

    private fun storeSuccessfulCharge(successfullyCharged: SuccessfullyCharged): Int {
        return dal.updateInvoice(successfullyCharged.invoice.id, successfullyCharged.invoice.status,
            successfullyCharged.eventName, successfullyCharged.eventTime)
    }


    private fun storePaymentException(paymentException: PaymentException): Int {
        return dal.updateInvoice(paymentException.invoice.id, paymentException.invoice.status,
            paymentException.eventName, paymentException.eventTime)
    }


    fun processPaymentResult(paymentResult: Either<PaymentException, SuccessfullyCharged>): Either<PaymentException, SuccessfullyCharged> {
        return paymentResult.bimap({storePaymentException(it); it},{storeSuccessfulCharge(it);it})
   }
}
