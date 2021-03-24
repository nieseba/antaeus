package io.pleo.antaeus.core.external

import io.pleo.antaeus.models.Invoice
import java.time.Instant

data class SuccessfullyCharged(val invoice: Invoice) {
    val eventName = "successfully-charged"
    val eventTime: Instant = Instant.now()
}