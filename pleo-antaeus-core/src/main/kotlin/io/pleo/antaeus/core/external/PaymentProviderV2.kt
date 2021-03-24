package io.pleo.antaeus.core.external

import arrow.core.*
import io.pleo.antaeus.core.exceptions.*
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus

class PaymentProviderV2(private val x: PaymentProvider) {

    fun charge(invoice: Invoice): Either<PaymentException, SuccessfullyCharged> {
        return try {
            val charged = x.charge(invoice)
            if (charged) Right(SuccessfullyCharged(invoice.copy(status = InvoiceStatus.PAID))) else Left(CustomerAccountDidNotAllowChargePaymentException(invoice))
        } catch (e: Exception) {
            when(e) {
                is NetworkException -> Left(NetworkPaymentException(invoice))
                is CustomerNotFoundException -> Left(CustomerNotFoundPaymentException(invoice.copy(status = InvoiceStatus.FAILED)))
                is CurrencyMismatchException -> Left(CurrencyMismatchPaymentException(invoice.copy(status = InvoiceStatus.FAILED)))
                else -> throw e
            }
        }
    }

}
