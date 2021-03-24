package io.pleo.antaeus.core.services

import arrow.core.Either
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.exceptions.CustomerNotFoundPaymentException
import io.pleo.antaeus.core.exceptions.InvoiceNotFoundException
import io.pleo.antaeus.core.exceptions.NetworkPaymentException
import io.pleo.antaeus.core.external.SuccessfullyCharged
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Money
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal

class InvoiceServiceTest {
    private val dal = mockk<AntaeusDal> {
        every { fetchInvoice(404) } returns null
    }

    private val invoiceService = InvoiceService(dal = dal)

    @Test
    fun `will throw if invoice is not found`() {
        assertThrows<InvoiceNotFoundException> {
            invoiceService.fetch(404)
        }
    }

    @Test
    fun `will process successful payment and mark invoice as paid`() {
        val invoice1 = Invoice(1, 1, Money(BigDecimal(1), Currency.DKK), InvoiceStatus.PENDING)
        val successfullyCharged = SuccessfullyCharged(invoice1.copy(status = InvoiceStatus.PAID))
        every { dal.updateInvoice(1, InvoiceStatus.PAID, any(), any())} returns 1

        invoiceService.processPaymentResult(Either.Right(successfullyCharged))

        verify(exactly = 1) {
            dal.updateInvoice(1, InvoiceStatus.PAID, successfullyCharged.eventName, any())
        }

    }

    @Test
    fun `will process NetworkError exception, not mark invoice as failed and keep trace of error `() {
        val invoice1 = Invoice(1, 1, Money(BigDecimal(1), Currency.DKK), InvoiceStatus.PENDING)
        val networkPaymentException = NetworkPaymentException(invoice1)

        every { dal.updateInvoice(1, InvoiceStatus.PENDING, any(), any())} returns 1
        every { dal.createInvoiceEvent(1, InvoiceStatus.PENDING, any())} returns 1

        invoiceService.processPaymentResult(Either.Left(networkPaymentException))

        verify(exactly = 1) {
//            dal.createInvoiceEvent(1, InvoiceStatus.PENDING, any())
            dal.updateInvoice(1, InvoiceStatus.PENDING, any(), any())
        }
        verify(exactly = 0) {
            dal.updateInvoice(1, InvoiceStatus.PAID, any(), any())
            dal.updateInvoice(1, InvoiceStatus.FAILED, any(), any())

        }
    }

    @Test
    fun `will process CustomerNotFoundPaymentException exception, mark invoice as failed and keep trace of error `() {
        val invoice1 = Invoice(1, 1, Money(BigDecimal(1), Currency.DKK), InvoiceStatus.PENDING)
        val customerNotFoundException = CustomerNotFoundPaymentException(invoice1.copy(status = InvoiceStatus.FAILED))
        every { dal.updateInvoice(1, any(), any(), any())} returns 1

        invoiceService.processPaymentResult(Either.Left(customerNotFoundException))

        verify(exactly = 1) {
            dal.updateInvoice(1, InvoiceStatus.FAILED, any(), any())
        }
        verify(exactly = 0) {
            dal.updateInvoice(1, InvoiceStatus.PAID, any(), any())

        }
    }
}
