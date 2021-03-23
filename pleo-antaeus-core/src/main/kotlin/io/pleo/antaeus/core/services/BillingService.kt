package io.pleo.antaeus.core.services

import arrow.core.Either
import io.pleo.antaeus.core.exceptions.*
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.core.external.PaymentProviderV2
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus


data class SuccessfullyCharged(
    val invoice: Invoice
)

class BillingService(
    private val paymentProvider: PaymentProvider,
    private val invoiceService: InvoiceService
) {

    private val paymentProviderV2 = PaymentProviderV2(paymentProvider)

    fun chargeForPendingInvoices(): List<Either<PaymentException, SuccessfullyCharged>> {
        return invoiceService.fetch(InvoiceStatus.PENDING)
            .map { i: Invoice ->
                paymentProviderV2.charge(i)
                    .map { invoiceService.markInvoiceAsPaid(it); SuccessfullyCharged(i) }
                    .mapLeft {
                        when (it) {
                            is CurrencyMismatchPaymentException ->  {invoiceService.markInvoiceAsFailed(it.invoice.id, it.name);it}
                            is CustomerNotFoundPaymentException -> {invoiceService.markInvoiceAsFailed(it.invoice.id, it.name);it}
                            is CustomerAccountDidAllowChargePaymentException -> {invoiceService.traceRetriableError(it.invoice.id, it.name);it}
                            is NetworkPaymentException -> {invoiceService.traceRetriableError(it.invoice.id,it.name);it}
                        }
                    }
            }
    }
}
