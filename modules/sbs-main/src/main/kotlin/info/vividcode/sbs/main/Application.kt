@file:JvmName("Application")
package info.vividcode.sbs.main

import io.ktor.application.Application
import io.ktor.application.ApplicationCallPipeline
import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.response.respondText

fun Application.setup() {
    intercept(ApplicationCallPipeline.Call) {
        call.respondText("Hello world!", ContentType.Text.Plain)
    }
}
