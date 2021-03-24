package io.pleo.antaeus.core.services

import arrow.core.Left
import arrow.core.Right
import arrow.core.left
import arrow.core.right
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.exceptions.CurrencyMismatchPaymentException
import io.pleo.antaeus.core.exceptions.CustomerAccountDidNotAllowChargePaymentException
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.core.external.PaymentProviderV2
import io.pleo.antaeus.core.external.SuccessfullyCharged
import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Money
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class BillingServiceTest {

    private val paymentProvider = mockk<PaymentProviderV2>()

    private val invoiceService = mockk<InvoiceService>()

    @Test
    fun `will charge payments for pending invoices`() {
        val invoice1 = Invoice(1, 1, Money(BigDecimal(1), Currency.DKK), InvoiceStatus.PENDING)
        val invoice2 = Invoice(2, 2, Money(BigDecimal(1), Currency.DKK), InvoiceStatus.PENDING)

        val successfullyCharged1 = SuccessfullyCharged(invoice1).right()
        val successfullyCharged2 = SuccessfullyCharged(invoice2).right()

        every { invoiceService.fetch(InvoiceStatus.PENDING) } returns listOf(
            invoice1, invoice2
        )
        every { paymentProvider.charge(invoice1) } returns successfullyCharged1
        every { paymentProvider.charge(invoice2) } returns successfullyCharged2

        every { invoiceService.processPaymentResult(successfullyCharged1) } returns successfullyCharged1

        every { invoiceService.processPaymentResult(successfullyCharged2) } returns successfullyCharged2


        val billingService = BillingService(paymentProvider, invoiceService)
        val paidInvoices = billingService.chargeForPendingInvoices()
        Assertions.assertEquals(
            listOf(
                successfullyCharged1,
                successfullyCharged2
            ), paidInvoices
        )

        verify(exactly = 1) {
            paymentProvider.charge(invoice1)
            invoiceService.processPaymentResult(successfullyCharged1)

            paymentProvider.charge(invoice2)
            invoiceService.processPaymentResult(successfullyCharged2)
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

        val successfullyCharged1 = SuccessfullyCharged(invoice1).right()
        val currencyMismatchPaymentException = CurrencyMismatchPaymentException(invoice2).left()
        val successfullyCharged3 = SuccessfullyCharged(invoice3).right()

        every { paymentProvider.charge(invoice1) } returns successfullyCharged1
        every { paymentProvider.charge(invoice2) } returns currencyMismatchPaymentException
        every { paymentProvider.charge(invoice3) } returns successfullyCharged3

        every { invoiceService.processPaymentResult(successfullyCharged1) } returns successfullyCharged1
        every { invoiceService.processPaymentResult(currencyMismatchPaymentException) } returns currencyMismatchPaymentException
        every { invoiceService.processPaymentResult(successfullyCharged3) } returns successfullyCharged3


        val billingService = BillingService(paymentProvider, invoiceService)
        val paidInvoices = billingService.chargeForPendingInvoices()
        Assertions.assertEquals(
            listOf(
                successfullyCharged1,
                currencyMismatchPaymentException,
                successfullyCharged3
            ), paidInvoices
        )

        verify(exactly = 1) {
            paymentProvider.charge(invoice1)
            invoiceService.processPaymentResult(successfullyCharged1)

            paymentProvider.charge(invoice2)
            invoiceService.processPaymentResult(currencyMismatchPaymentException)

            paymentProvider.charge(invoice3)
            invoiceService.processPaymentResult(successfullyCharged3)
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
        
        val customerAccountChargeException = CustomerAccountDidNotAllowChargePaymentException(invoice1).left()
        val currencyMismatchPaymentException = CurrencyMismatchPaymentException(invoice2).left()
        val successfullyCharged3 = SuccessfullyCharged(invoice3).right()

        every { paymentProvider.charge(invoice1) } returns customerAccountChargeException
        every { paymentProvider.charge(invoice2) } returns currencyMismatchPaymentException
        every { paymentProvider.charge(invoice3) } returns successfullyCharged3

        every { invoiceService.processPaymentResult(customerAccountChargeException) } returns customerAccountChargeException
        every { invoiceService.processPaymentResult(currencyMismatchPaymentException) } returns currencyMismatchPaymentException
        every { invoiceService.processPaymentResult(successfullyCharged3) } returns successfullyCharged3

        val billingService = BillingService(paymentProvider, invoiceService)
        val paidInvoices = billingService.chargeForPendingInvoices()
        Assertions.assertEquals(
            listOf(
                customerAccountChargeException,
                currencyMismatchPaymentException,
                successfullyCharged3
            ), paidInvoices
        )

        verify(exactly = 1) {
            paymentProvider.charge(invoice1)
            paymentProvider.charge(invoice2)
            paymentProvider.charge(invoice3)

            invoiceService.processPaymentResult(customerAccountChargeException)
            invoiceService.processPaymentResult(currencyMismatchPaymentException)
            invoiceService.processPaymentResult(successfullyCharged3)

        }

    }




}