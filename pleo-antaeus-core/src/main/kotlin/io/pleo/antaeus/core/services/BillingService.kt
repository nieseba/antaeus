package io.pleo.antaeus.core.services

import arrow.core.Either
import io.pleo.antaeus.core.exceptions.*
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.core.external.PaymentProviderV2
import io.pleo.antaeus.core.external.SuccessfullyCharged
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus


class BillingService(
    private val paymentProvider: PaymentProviderV2,
    private val invoiceService: InvoiceService
) {

    fun chargeForPendingInvoices(): List<Either<PaymentException, SuccessfullyCharged>> {
        return invoiceService.fetch(InvoiceStatus.PENDING)
            .map { invoiceService.processPaymentResult(paymentProvider.charge(it)) }
    }
}

