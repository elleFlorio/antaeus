package io.pleo.antaeus.core.notification

import io.pleo.antaeus.models.Invoice

data class Message(val invoice: Invoice, val outcome: OutcomeType)