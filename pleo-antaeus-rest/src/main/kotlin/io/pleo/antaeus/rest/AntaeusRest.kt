/*
    Configures the rest app along with basic exception handling and URL endpoints.
 */

package io.pleo.antaeus.rest

import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.*
import io.pleo.antaeus.core.exceptions.EntityNotFoundException
import io.pleo.antaeus.core.services.CustomerService
import io.pleo.antaeus.core.services.InvoiceService
import io.pleo.antaeus.core.services.billing.BillingService
import io.pleo.antaeus.validation.Validator
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

class AntaeusRest (
    private val invoiceService: InvoiceService,
    private val customerService: CustomerService,
    private val billingService: BillingService
) : Runnable {

    override fun run() {
        app.start(7000)
    }

    // Set up Javalin rest app
    private val app = Javalin
        .create()
        .apply {
            // InvoiceNotFoundException: return 404 HTTP status code
            exception(EntityNotFoundException::class.java) { _, ctx ->
                ctx.status(404)
            }
            // Unexpected exception: return HTTP 500
            exception(Exception::class.java) { e, _ ->
                logger.error(e) { "Internal server error" }
            }
            // On 404: return message
            error(404) { ctx -> ctx.json("not found") }
        }

    init {
        // Set up URL endpoints for the rest app
        app.routes {
           path("rest") {
               // Route to check whether the app is running
               // URL: /rest/health
               get("health") {
                   it.json("ok")
               }

               // V1
               path("v1") {
                   path("invoices") {
                       // URL: /rest/v1/invoices
                       get {
                           it.json(invoiceService.fetchAll())
                       }

                       // URL: /rest/v1/invoices/{:id}
                       get(":id") {
                          it.json(invoiceService.fetch(it.pathParam("id").toInt()))
                       }
                   }

                   path("customers") {
                       // URL: /rest/v1/customers
                       get {
                           it.json(customerService.fetchAll())
                       }

                       // URL: /rest/v1/customers/{:id}
                       get(":id") {
                           it.json(customerService.fetch(it.pathParam("id").toInt()))
                       }
                   }

                   path("billing") {
                       post {
                           if (Validator.isValidInvoiceId(it.body())) {
                               it.json(billingService.requestPayment(it.body().toInt()))
                           } else {
                               it.status(400)
                           }
                       }

                       path("periodic") {
                           get {
                               it.json(billingService.isPeriodicBillingActive())
                           }

                           post {
                               billingService.startPeriodicBilling()
                               it.status(200)
                           }

                           put {
                               if (Validator.isValidPeriodicScheduling(it.body())) {
                                   val (day, hour, minute) = it.body().split(":")
                                   billingService.setPeriodicBilling(day, hour, minute)
                                   it.status(200)
                               } else {
                                   it.status(400)
                               }

                           }

                           delete {
                               billingService.cancelPeriodicBilling()
                               it.status(200)
                           }
                       }
                   }
               }
           }
        }
    }
}
