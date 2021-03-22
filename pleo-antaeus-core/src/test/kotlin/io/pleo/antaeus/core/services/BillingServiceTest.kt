package io.pleo.antaeus.core.services

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
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
            paidInvoices, listOf(
                ChargeResults(1, true),
                ChargeResults(2, true)
            )
        )

        verify(exactly = 1) {
            paymentProvider.charge(invoice1)
            invoiceService.markInvoiceAsPaid(invoice1.id)

            paymentProvider.charge(invoice2)
            invoiceService.markInvoiceAsPaid(invoice2.id)
        }
    }



}