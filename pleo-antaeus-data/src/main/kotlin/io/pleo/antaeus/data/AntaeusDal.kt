/*
    Implements the data access layer (DAL).
    The data access layer generates and executes requests to the database.

    See the `mappings` module for the conversions between database rows and Kotlin objects.
 */

package io.pleo.antaeus.data

import io.pleo.antaeus.models.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant

class AntaeusDal(private val db: Database) {
    fun fetchInvoice(id: Int): Invoice? {
        // transaction(db) runs the internal query as a new database transaction.
        return transaction(db) {
            // Returns the first invoice with matching id.
            InvoiceTable
                .select { InvoiceTable.id.eq(id) }
                .firstOrNull()
                ?.toInvoice()
        }
    }

    fun fetchInvoices(): List<Invoice> {
        return transaction(db) {
            InvoiceTable
                .selectAll()
                .map { it.toInvoice() }
        }
    }

    fun fetchInvoicesByStatus(status: InvoiceStatus): List<Invoice> {
        return transaction(db) {
            InvoiceTable
                    .select{ InvoiceTable.status.eq(status.name) }
                    .map { it.toInvoice() }
        }

    }

    fun updateInvoice(id: Int, status: InvoiceStatus, eventType: String): Int {
        return transaction(db) {
            InvoiceTable
                .update({ InvoiceTable.id.eq(id) }) {
                    it[InvoiceTable.status] = status.toString()
                }
            InvoiceEventTable.insert {
                it[this.invoiceId] = id
                it[this.status] = status.toString()
                it[this.eventType] = eventType
                it[this.eventTime] = org.joda.time.Instant(Instant.now().toEpochMilli()).toDateTime()
            } get InvoiceEventTable.id
        }
    }

    fun createInvoiceEvent(id: Int, status: InvoiceStatus, eventType: String): Int {
        return transaction(db) {
            InvoiceEventTable.insert {
                it[this.invoiceId] = id
                it[this.status] = status.toString()
                it[this.eventType] = eventType
                it[this.eventTime] = org.joda.time.Instant(Instant.now().toEpochMilli()).toDateTime()
            } get InvoiceEventTable.id
        }
    }


    fun createInvoice(amount: Money, customer: Customer, status: InvoiceStatus = InvoiceStatus.PENDING): Invoice? {
        val id = transaction(db) {
            // Insert the invoice and returns its new id.
            val invoiceId = InvoiceTable
                .insert {
                    it[this.value] = amount.value
                    it[this.currency] = amount.currency.toString()
                    it[this.status] = status.toString()
                    it[this.customerId] = customer.id
                } get InvoiceTable.id
            InvoiceEventTable.insert {
                it[this.value] = amount.value
                it[this.currency] = amount.currency.toString()
                it[this.status] = status.toString()
                it[this.customerId] = customer.id
                it[this.invoiceId] = invoiceId
                it[this.eventType] = "created"
                it[this.eventTime] = org.joda.time.Instant(Instant.now().toEpochMilli()).toDateTime()
            } get InvoiceEventTable.id
            invoiceId
        }

        return fetchInvoice(id)
    }

    fun fetchCustomer(id: Int): Customer? {
        return transaction(db) {
            CustomerTable
                .select { CustomerTable.id.eq(id) }
                .firstOrNull()
                ?.toCustomer()
        }
    }

    fun fetchCustomers(): List<Customer> {
        return transaction(db) {
            CustomerTable
                .selectAll()
                .map { it.toCustomer() }
        }
    }

    fun createCustomer(currency: Currency): Customer? {
        val id = transaction(db) {
            // Insert the customer and return its new id.
            CustomerTable.insert {
                it[this.currency] = currency.toString()
            } get CustomerTable.id
        }

        return fetchCustomer(id)
    }

    fun fetchInvoiceEvents(invoiceId: Int): List<InvoiceEvent> {
        return transaction(db) {
            InvoiceEventTable
                .select { InvoiceEventTable.invoiceId.eq(invoiceId) }
                .map { it.toInvoiceEvent() }
        }
    }

}
