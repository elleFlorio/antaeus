package io.pleo.antaeus.core.services.billing

import io.pleo.antaeus.core.notification.OutcomeType
import io.pleo.antaeus.models.Invoice

data class PaymentRequestOutcome(val invoice: Invoice, val result: Boolean, val outcome: OutcomeType)