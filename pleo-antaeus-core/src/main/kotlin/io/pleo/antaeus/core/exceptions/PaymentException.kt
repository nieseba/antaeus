package io.pleo.antaeus.core.exceptions

import io.pleo.antaeus.models.Invoice
import java.time.Instant

sealed class PaymentException {
    abstract val invoice: Invoice
    abstract val eventName: String
    val eventTime: Instant = Instant.now()
}

data class CurrencyMismatchPaymentException(override val invoice: Invoice,
                                            override val eventName: String = "currency-mismatch"): PaymentException()

data class CustomerNotFoundPaymentException(override val invoice: Invoice,
                                            override val eventName: String = "customer-not-found"): PaymentException()

data class CustomerAccountDidNotAllowChargePaymentException(override val invoice: Invoice,
                                                            override val eventName: String = "customer-account-did-not-allow-charge"): PaymentException()

data class NetworkPaymentException(
    override val invoice: Invoice,
    override val eventName: String = "network-error"): PaymentException()

