package io.pleo.antaeus.data

import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Money
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import java.math.BigDecimal
import java.sql.Connection
import kotlin.random.Random

class AntaeusDalTest {


    val tables = arrayOf(InvoiceTable, CustomerTable, InvoiceEventTable)

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

    @BeforeEach
    fun initData() {
        // Drop all existing tables to ensure a clean slate on each run
        db.also {
            TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE
            transaction(it) {
                addLogger(StdOutSqlLogger)
                // Drop all existing tables to ensure a clean slate on each run
                SchemaUtils.drop(*tables)
                // Create all tables
                SchemaUtils.create(*tables)
            }
        }

        val customers = (1..10).mapNotNull {
            dal.createCustomer(
                currency = Currency.values()[Random.nextInt(0, Currency.values().size)]
            )
        }

        customers.forEach { customer ->
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
        dal.updateInvoice(pendingInvoices[0].id, InvoiceStatus.PAID, "Successful")
        val newStatus = dal.fetchInvoice(pendingInvoices[0].id)?.status
        assertEquals(newStatus, InvoiceStatus.PAID)
    }

    @Test
    fun `mark invoice as failed should make status field in db`() {
        val pendingInvoices = dal.fetchInvoicesByStatus(status = InvoiceStatus.PENDING)
        dal.updateInvoice(pendingInvoices[0].id, InvoiceStatus.FAILED, "ERROR")
        val newStatus = dal.fetchInvoice(pendingInvoices[0].id)?.status
        assertEquals(newStatus, InvoiceStatus.FAILED)
    }

    @Test
    fun `create entry in invoice event log during an invoice creation`() {
        val pendingInvoices = dal.fetchInvoicesByStatus(status = InvoiceStatus.PENDING)
        val invoiceEvents = dal.fetchInvoiceEvents(pendingInvoices[0].id)
        assertEquals(1, invoiceEvents.size)
        assertEquals(pendingInvoices[0].customerId, invoiceEvents[0].customerId)
        assertEquals(pendingInvoices[0].id, invoiceEvents[0].invoiceId)
        assertEquals(InvoiceStatus.PENDING,invoiceEvents[0].status)
    }

    @Test
    fun `mark invoice as failed should create entry in event log`() {
        val pendingInvoices = dal.fetchInvoicesByStatus(status = InvoiceStatus.PENDING)
        dal.updateInvoice(pendingInvoices[0].id, InvoiceStatus.FAILED, "ERROR")

        val invoiceEvents = dal.fetchInvoiceEvents(pendingInvoices[0].id)
        assertEquals(2, invoiceEvents.size)
        assertEquals(InvoiceStatus.PENDING, invoiceEvents[0].status)
        assertEquals(InvoiceStatus.FAILED, invoiceEvents[1].status)
    }


}