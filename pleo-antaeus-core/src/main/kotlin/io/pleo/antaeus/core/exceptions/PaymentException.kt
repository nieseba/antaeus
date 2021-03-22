package io.pleo.antaeus.core.exceptions

import io.pleo.antaeus.models.Invoice

sealed class PaymentException {
    abstract val invoice: Invoice
}

data class CurrencyMismatchPaymentException(override val invoice: Invoice): PaymentException()
data class CustomerNotFoundPaymentException(override val invoice: Invoice): PaymentException()
data class CustomerAccountDidAllowChargePaymentException(override val invoice: Invoice): PaymentException()
data class NetworkPaymentException(override val invoice: Invoice): PaymentException()

