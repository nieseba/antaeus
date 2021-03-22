package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.models.InvoiceStatus

data class ChargeResults(
        val invoiceId: Int,
        val charged: Boolean
        )

class BillingService(
    private val paymentProvider: PaymentProvider,
    private val invoiceService: InvoiceService
) {
// TODO - Add code e.g. here
    fun chargeForPendingInvoices(): List<ChargeResults> {
        val pendingInvoices = invoiceService.fetch(InvoiceStatus.PENDING)
        val chargedResults = pendingInvoices.map{ ChargeResults(it.id, paymentProvider.charge(it))}
//      TODO - naive filtering of successful payments
        val successfulCharges = chargedResults.filter { it.charged }
        successfulCharges.map { invoiceService.markInvoiceAsPaid(it.invoiceId) }
        return chargedResults
}
}
