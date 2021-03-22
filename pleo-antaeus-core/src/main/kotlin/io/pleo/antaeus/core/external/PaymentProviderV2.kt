package io.pleo.antaeus.core.external

import arrow.core.*
import io.pleo.antaeus.core.exceptions.*
import io.pleo.antaeus.models.Invoice

class PaymentProviderV2(private val x: PaymentProvider) {

    fun charge(invoice: Invoice): Either<PaymentException, Int> {
        return try {
            val charged = x.charge(invoice)
            if (charged) Right(invoice.id) else Left(CustomerAccountDidAllowChargePaymentException(invoice))
        } catch (e: Exception) {
            when(e) {
                is NetworkException -> Left(NetworkPaymentException(invoice))
                is CustomerNotFoundException -> Left(CustomerNotFoundPaymentException(invoice))
                is CurrencyMismatchException -> Left(CurrencyMismatchPaymentException(invoice))
                else -> throw e
            }
        }
    }

}
