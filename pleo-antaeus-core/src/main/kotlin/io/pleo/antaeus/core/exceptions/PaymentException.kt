package io.pleo.antaeus.core.exceptions

import io.pleo.antaeus.models.Invoice

sealed class PaymentException {
    abstract val invoice: Invoice
    abstract val name: String
}

data class CurrencyMismatchPaymentException(override val invoice: Invoice,
                                            override val name: String = "currency-mismatch"): PaymentException()

data class CustomerNotFoundPaymentException(override val invoice: Invoice,
                                            override val name: String = "customer-not-found"): PaymentException()

data class CustomerAccountDidAllowChargePaymentException(override val invoice: Invoice,
                                                         override val name: String = "customer-account-did-not-allow-charge"): PaymentException()

data class NetworkPaymentException(
    override val invoice: Invoice,
    override val name: String = "network-error"): PaymentException()

