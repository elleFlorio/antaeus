
import io.pleo.antaeus.core.external.CurrencyConversionProvider
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.core.notification.Message
import io.pleo.antaeus.core.notification.NotificationService
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Money
import mu.KotlinLogging
import java.math.BigDecimal
import kotlin.random.Random

private val logger = KotlinLogging.logger {}

// This will create all schemas and setup initial data
internal fun setupInitialData(dal: AntaeusDal) {
    val customers = (1..100).mapNotNull {
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

// This is the mocked instance of the payment provider
internal fun getPaymentProvider(): PaymentProvider {
    return object : PaymentProvider {
        override fun charge(invoice: Invoice): Boolean {
                return Random.nextBoolean()
        }
    }
}

// Mocked instance of the currency conversion provider
internal fun getCurrencyConversionProvider(): CurrencyConversionProvider {
    return object: CurrencyConversionProvider {
        override fun convertCurrency(source: Money, target: Currency): Money {
            return Money(BigDecimal(Random.nextInt(10, 1000)), target)
        }
    }
}

// Mocked instance of the notification service
internal fun getNotficationService(): NotificationService {
    return object: NotificationService {
        override fun notifySuccess(message: Message) {
            logger.info { "Success message $message sent" }
        }
        override fun notifyFailure(message: Message) {
            logger.info { "Failure message $message sent" }
        }
    }
}