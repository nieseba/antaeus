package io.pleo.antaeus.core.external

import arrow.core.*
import io.mockk.every
import io.mockk.mockk
import io.pleo.antaeus.core.exceptions.*
import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Money
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class PaymentProviderV2Test {

    private val paymentProvider = mockk<PaymentProvider>()

    private val paymentProcessV2 = PaymentProviderV2(paymentProvider)

    @Test
    fun `will transform successful payment into Either structure`() {
        val invoice1 = Invoice(1, 1, Money(BigDecimal(1), Currency.DKK), InvoiceStatus.PENDING)

        every { paymentProvider.charge(invoice1) } returns true
        assertEquals(paymentProcessV2.charge(invoice1), (invoice1.id).right())
    }

    @Test
    fun `will transform not successful payment (account balance did not allow the charge) into Either structure`() {
        val invoice1 = Invoice(1, 1, Money(BigDecimal(1), Currency.DKK), InvoiceStatus.PENDING)

        every { paymentProvider.charge(invoice1) } returns false
        paymentProcessV2.charge(invoice1)
        assertEquals(paymentProcessV2.charge(invoice1), CustomerAccountDidAllowChargePaymentException(invoice1).left())
    }

    @Test
    fun `will transform not successful payment (currency mismatch exception) into Either structure`() {
        val invoice1 = Invoice(1, 1, Money(BigDecimal(1), Currency.DKK), InvoiceStatus.PENDING)

        every { paymentProvider.charge(invoice1) } throws CurrencyMismatchException(invoice1.id, invoice1.customerId)
        paymentProcessV2.charge(invoice1)
        assertEquals(paymentProcessV2.charge(invoice1), CurrencyMismatchPaymentException(invoice1).left())
    }

    @Test
    fun `will transform not successful payment (network exception) into Either structure`() {
        val invoice1 = Invoice(1, 1, Money(BigDecimal(1), Currency.DKK), InvoiceStatus.PENDING)

        every { paymentProvider.charge(invoice1) } throws NetworkException()
        paymentProcessV2.charge(invoice1)
        assertEquals(paymentProcessV2.charge(invoice1), NetworkPaymentException(invoice1).left())

    }
}