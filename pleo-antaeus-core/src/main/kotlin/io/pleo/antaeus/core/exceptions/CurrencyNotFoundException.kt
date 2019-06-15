package io.pleo.antaeus.core.exceptions

import io.pleo.antaeus.models.Currency

class CurrencyNotFoundException(currency: Currency) :
    Exception("Currency '$currency' not found")
