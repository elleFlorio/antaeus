package io.pleo.antaeus.core.notification

interface NotificationService {

    fun notifySuccess(message: Message)
    fun notifyFailure(message: Message)
}