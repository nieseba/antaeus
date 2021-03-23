package io.pleo.antaeus.models

import java.time.Instant

data class InvoiceEvent(
    val id: Int?,
    val invoiceId: Int,
    val customerId: Int?,
    val amount: Money?,
    val status: InvoiceStatus?,
    val eventType: String,
    val eventTime: Instant
)