/*
    Defines database tables and their schemas.
    To be used by `AntaeusDal`.
 */

package io.pleo.antaeus.data

import io.pleo.antaeus.data.InvoiceTable.autoIncrement
import io.pleo.antaeus.data.InvoiceTable.primaryKey
import org.jetbrains.exposed.sql.Table

object InvoiceTable : Table() {
    val id = integer("id").autoIncrement().primaryKey()
    val currency = varchar("currency", 3)
    val value = decimal("value", 1000, 2)
    val customerId = reference("customer_id", CustomerTable.id)
    val status = text("status")
}

object CustomerTable : Table() {
    val id = integer("id").autoIncrement().primaryKey()
    val currency = varchar("currency", 3)
}

object InvoiceEventTable: Table() {
    val id = integer("id").autoIncrement().primaryKey()
    val invoiceId = reference("invoice_id", InvoiceTable.id)
    val eventType = text("eventType")
    val eventTime = datetime("eventTime")
    val currency = varchar("currency", 3).nullable()
    val value = decimal("value", 1000, 2).nullable()
    val customerId = reference("customer_id", CustomerTable.id).nullable()
    val status = text("status").nullable()

}
