/*
    Defines mappings between database rows and Kotlin objects.
    To be used by `AntaeusDal`.
 */

package io.pleo.antaeus.data

import io.pleo.antaeus.models.*
import io.pleo.antaeus.models.Currency
import org.jetbrains.exposed.sql.ResultRow

fun ResultRow.toInvoice(): Invoice = Invoice(
    id = this[InvoiceTable.id],
    amount = Money(
        value = this[InvoiceTable.value],
        currency = Currency.valueOf(this[InvoiceTable.currency])
    ),
    status = InvoiceStatus.valueOf(this[InvoiceTable.status]),
    customerId = this[InvoiceTable.customerId]
)

fun ResultRow.toCustomer(): Customer = Customer(
    id = this[CustomerTable.id],
    currency = Currency.valueOf(this[CustomerTable.currency])
)

fun ResultRow.toInvoiceEvent(): InvoiceEvent {
    val value = this[InvoiceEventTable.value]
    val currency = this[InvoiceEventTable.currency]
    val money = if (value != null && currency != null) {
            Money(value, Currency.valueOf(currency))
        } else null
    val statusRaw = this[InvoiceEventTable.status]
    val status = if (statusRaw != null) InvoiceStatus.valueOf(statusRaw) else null

    return InvoiceEvent(
        id = this[InvoiceEventTable.id],
        amount = money,
        status = status,
        customerId = this[InvoiceEventTable.customerId],
        invoiceId = this[InvoiceEventTable.invoiceId],
        eventType = this[InvoiceEventTable.eventType],
        eventTime = java.time.Instant.ofEpochMilli(this[InvoiceEventTable.eventTime].millis)
    ) }

