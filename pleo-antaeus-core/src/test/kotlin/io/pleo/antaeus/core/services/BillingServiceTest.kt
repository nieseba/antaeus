package io.pleo.antaeus.core.services

import arrow.core.Left
import arrow.core.Right
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.exceptions.CurrencyMismatchPaymentException
import io.pleo.antaeus.core.exceptions.CustomerAccountDidAllowChargePaymentException
import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Money
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class BillingServiceTest {

    private val paymentProvider = mockk<PaymentProvider>()

    private val invoiceService = mockk<InvoiceService>()

    @Test
    fun `will charge payments for pending invoices`() {
        val invoice1 = Invoice(1, 1, Money(BigDecimal(1), Currency.DKK), InvoiceStatus.PENDING)
        val invoice2 = Invoice(2, 2, Money(BigDecimal(1), Currency.DKK), InvoiceStatus.PENDING)
        every { invoiceService.fetch(InvoiceStatus.PENDING) } returns listOf(
            invoice1, invoice2
        )
        every { paymentProvider.charge(invoice1) } returns true
        every { paymentProvider.charge(invoice2) } returns true

        every { invoiceService.markInvoiceAsPaid(invoice1.id) } returns 1
        every { invoiceService.markInvoiceAsPaid(invoice2.id) } returns 1


        val billingService = BillingService(paymentProvider, invoiceService)
        val paidInvoices = billingService.chargeForPendingInvoices()
        Assertions.assertEquals(
            listOf(
                Right(SuccessfullyCharged(invoice1)),
                Right(SuccessfullyCharged(invoice2))
            ), paidInvoices
        )

        verify(exactly = 1) {
            paymentProvider.charge(invoice1)
            invoiceService.markInvoiceAsPaid(invoice1.id)

            paymentProvider.charge(invoice2)
            invoiceService.markInvoiceAsPaid(invoice2.id)
        }
    }

    @Test
    fun `will continue processing if there's Currency Mismatch Exception`() {
        val invoice1 = Invoice(1, 1, Money(BigDecimal(1), Currency.DKK), InvoiceStatus.PENDING)
        val invoice2 = Invoice(2, 2, Money(BigDecimal(1), Currency.DKK), InvoiceStatus.PENDING)
        val invoice3 = Invoice(3, 3, Money(BigDecimal(1), Currency.DKK), InvoiceStatus.PENDING)

        every { invoiceService.fetch(InvoiceStatus.PENDING) } returns listOf(
            invoice1, invoice2, invoice3
        )
        every { paymentProvider.charge(invoice1) } returns true
        every { paymentProvider.charge(invoice2) } throws CurrencyMismatchException(invoice2.id, invoice2.customerId)
        every { paymentProvider.charge(invoice3) } returns true

        every { invoiceService.markInvoiceAsPaid(invoice1.id) } returns 1
        every { invoiceService.markInvoiceAsPaid(invoice3.id) } returns 1
        every { invoiceService.markInvoiceAsFailed(invoice2.id) } returns 1

        val billingService = BillingService(paymentProvider, invoiceService)
        val paidInvoices = billingService.chargeForPendingInvoices()
        Assertions.assertEquals(
            listOf(
                Right(SuccessfullyCharged(invoice1)),
                Left(CurrencyMismatchPaymentException(invoice2)),
                Right(SuccessfullyCharged(invoice3))
            ), paidInvoices
        )

        verify(exactly = 1) {
            paymentProvider.charge(invoice1)
            invoiceService.markInvoiceAsPaid(invoice1.id)

            paymentProvider.charge(invoice3)
            invoiceService.markInvoiceAsPaid(invoice3.id)
        }
    }

    @Test
    fun `will continue processing if there's Customer Account Did Not Allow Charge Mismatch exception`() {
        val invoice1 = Invoice(1, 1, Money(BigDecimal(1), Currency.DKK), InvoiceStatus.PENDING)
        val invoice2 = Invoice(2, 2, Money(BigDecimal(1), Currency.DKK), InvoiceStatus.PENDING)
        val invoice3 = Invoice(3, 3, Money(BigDecimal(1), Currency.DKK), InvoiceStatus.PENDING)

        every { invoiceService.fetch(InvoiceStatus.PENDING) } returns listOf(
            invoice1, invoice2, invoice3
        )
        every { paymentProvider.charge(invoice1) } returns false
        every { paymentProvider.charge(invoice2) } throws CurrencyMismatchException(invoice2.id, invoice2.customerId)
        every { paymentProvider.charge(invoice3) } returns true

        every { invoiceService.markInvoiceAsPaid(invoice3.id) } returns 1
        every { invoiceService.markInvoiceAsFailed(invoice2.id) } returns 1


        val billingService = BillingService(paymentProvider, invoiceService)
        val paidInvoices = billingService.chargeForPendingInvoices()
        Assertions.assertEquals(
            listOf(
                Left(CustomerAccountDidAllowChargePaymentException(invoice1)),
                Left(CurrencyMismatchPaymentException(invoice2)),
                Right(SuccessfullyCharged(invoice3))
            ), paidInvoices
        )

        verify(exactly = 1) {
            paymentProvider.charge(invoice1)
            paymentProvider.charge(invoice2)
            paymentProvider.charge(invoice3)
            invoiceService.markInvoiceAsPaid(invoice3.id)
        }

        verify(exactly = 0) {
            invoiceService.markInvoiceAsPaid(invoice1.id)
            invoiceService.markInvoiceAsPaid(invoice2.id)
        }
    }

    @Test
    fun `will not mark invoices as failed after a NetworkError`() {
        val invoice1 = Invoice(1, 1, Money(BigDecimal(1), Currency.DKK), InvoiceStatus.PENDING)
        val invoice2 = Invoice(2, 2, Money(BigDecimal(1), Currency.DKK), InvoiceStatus.PENDING)
        every { invoiceService.fetch(InvoiceStatus.PENDING) } returns listOf(
            invoice1, invoice2
        )
        every { paymentProvider.charge(invoice1) } returns true
        every { paymentProvider.charge(invoice2) } throws NetworkException()

        every { invoiceService.markInvoiceAsPaid(invoice1.id) } returns 1
        every { invoiceService.markInvoiceAsPaid(invoice2.id) } returns 1


        val billingService = BillingService(paymentProvider, invoiceService)
        val paidInvoices = billingService.chargeForPendingInvoices()

        verify(exactly = 1) {
            paymentProvider.charge(invoice1)
            invoiceService.markInvoiceAsPaid(invoice1.id)
            paymentProvider.charge(invoice2)

        }
        verify(exactly = 0) {
            invoiceService.markInvoiceAsPaid(invoice2.id)
        }
    }

    @Test
    fun `will mark invoices as failed after a CurrencyMismatchError`() {
        val invoice1 = Invoice(1, 1, Money(BigDecimal(1), Currency.DKK), InvoiceStatus.PENDING)
        val invoice2 = Invoice(2, 2, Money(BigDecimal(1), Currency.DKK), InvoiceStatus.PENDING)
        every { invoiceService.fetch(InvoiceStatus.PENDING) } returns listOf(
            invoice1, invoice2
        )
        every { paymentProvider.charge(invoice1) } returns true
        every { paymentProvider.charge(invoice2) } throws CurrencyMismatchException(invoice2.id, invoice2.customerId)

        every { invoiceService.markInvoiceAsPaid(invoice1.id) } returns 1
        every { invoiceService.markInvoiceAsPaid(invoice2.id) } returns 1
        every { invoiceService.markInvoiceAsFailed(invoice2.id) } returns 1


        val billingService = BillingService(paymentProvider, invoiceService)
        val paidInvoices = billingService.chargeForPendingInvoices()

        verify(exactly = 1) {
            paymentProvider.charge(invoice1)
            invoiceService.markInvoiceAsPaid(invoice1.id)
            paymentProvider.charge(invoice2)
            invoiceService.markInvoiceAsFailed(invoice2.id)

        }
        verify(exactly = 0) {
            invoiceService.markInvoiceAsPaid(invoice2.id)
        }
    }



}