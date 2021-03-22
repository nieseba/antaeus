package io.pleo.antaeus.data

import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Money
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.File
import java.math.BigDecimal
import java.sql.Connection
import kotlin.random.Random

class AntaeusDalTest {


    val tables = arrayOf(InvoiceTable, CustomerTable)

    val dbFile: File = File.createTempFile("antaeus-db", ".sqlite")
    // Connect to the database and create the needed tables. Drop any existing data.
    val db = Database
            .connect(url = "jdbc:sqlite:${dbFile.absolutePath}",
                    driver = "org.sqlite.JDBC",
                    user = "root",
                    password = "")
            .also {
                TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE
                transaction(it) {
                    addLogger(StdOutSqlLogger)
                    // Drop all existing tables to ensure a clean slate on each run
                    SchemaUtils.drop(*tables)
                    // Create all tables
                    SchemaUtils.create(*tables)
                }
            }

    // Set up data access layer.
    val dal = AntaeusDal(db = db)

    val customers = (1..10).mapNotNull {
        dal.createCustomer(
                currency = Currency.values()[Random.nextInt(0, Currency.values().size)]
        )
    }

    val notImportant = customers.forEach { customer ->
        (1..10).forEach {
            dal.createInvoice(
                    amount = Money(
                            value = BigDecimal(Random.nextDouble(10.0, 500.0)),
                            currency = customer.currency
                    ),
                    customer = customer,
                    status = if (it == 1) InvoiceStatus.PENDING else InvoiceStatus.PAID
            )
        }
    }


    @Test
    fun `fetch invoices by pending status should return correct invoices`() {
        val pendingInvoices = dal.fetchInvoicesByStatus(status = InvoiceStatus.PENDING)
        assertEquals(pendingInvoices.count(), 10)
        assertEquals(pendingInvoices.map{it.status}.toSet(), setOf(InvoiceStatus.PENDING))

    }

    @Test
    fun `fetch invoices by paid status should return correct invoices`() {
        val paidInvoices = dal.fetchInvoicesByStatus(status = InvoiceStatus.PAID)
        assertEquals(paidInvoices.count(), 90)
        assertEquals(paidInvoices.map{it.status}.toSet(), setOf(InvoiceStatus.PAID))
    }

    @Test
    fun `mark invoice as paid should make status field in db`() {
        val pendingInvoices = dal.fetchInvoicesByStatus(status = InvoiceStatus.PENDING)
        dal.updateInvoice(pendingInvoices[0].id, InvoiceStatus.PAID)
        val newStatus = dal.fetchInvoice(pendingInvoices[0].id)?.status
        assertEquals(newStatus, InvoiceStatus.PAID)
    }
}