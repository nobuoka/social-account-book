@file:JvmName("Application")

package info.vividcode.sbs.main

import info.vividcode.ktor.twitter.login.*
import info.vividcode.ktor.twitter.login.application.TemporaryCredentialNotFoundException
import info.vividcode.ktor.twitter.login.application.TwitterCallFailedException
import info.vividcode.oauth.OAuth
import info.vividcode.sbs.main.presentation.topHtml
import info.vividcode.sbs.main.presentation.withHtmlDoctype
import io.ktor.application.Application
import io.ktor.application.ApplicationCallPipeline
import io.ktor.application.call
import io.ktor.content.resources
import io.ktor.content.static
import io.ktor.content.staticBasePackage
import io.ktor.http.ContentType.Text.Html
import io.ktor.http.HttpStatusCode
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.response.respond
import io.ktor.response.respondRedirect
import io.ktor.response.respondWrite
import io.ktor.routing.get
import io.ktor.routing.routing
import kotlinx.coroutines.experimental.newFixedThreadPoolContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.time.Clock
import java.util.*
import kotlin.coroutines.experimental.CoroutineContext

fun Application.setup(env: Env? = null) {
    val twitterClientCredentials = environment.config.config("sbs.twitter.clientCredential").let {
        ClientCredential(it.property("identifier").getString(), it.property("sharedSecret").getString())
    }
    val appContextUrl = environment.config.property("sbs.contextUrl").getString()

    val envNotNull = env
            ?: object : Env {
                override val oauth: info.vividcode.oauth.OAuth = OAuth(object : OAuth.Env {
                    override val clock: Clock = Clock.systemDefaultZone()
                    override val nextInt: (Int) -> Int = Random()::nextInt
                })
                override val httpClient: okhttp3.Call.Factory = OkHttpClient.Builder()
                    .addNetworkInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BASIC))
                    .build()
                override val httpCallContext: CoroutineContext = newFixedThreadPoolContext(4, "HttpCall")
                override val temporaryCredentialStore: TemporaryCredentialStore =
                    TemporaryCredentialStore.OnMemoryTemporaryCredentialStore
            }

    intercept(ApplicationCallPipeline.Call) {
        try {
            proceed()
        } catch (e: Throwable) {
            e.printStackTrace()
            throw e
        }
    }

    routing {
        staticBasePackage = "sbs.main"
        static("static") {
            resources("static")
        }

        get(UrlPaths.top) {
            call.respondWrite(Html, OK, withHtmlDoctype(topHtml(UrlPaths.TwitterLogin.start)))
        }

        setupTwitterLogin(
            UrlPaths.TwitterLogin.start, UrlPaths.TwitterLogin.callback,
            appContextUrl, twitterClientCredentials, envNotNull,
            object : OutputPort {
                override val success: OutputInterceptor<TwitterToken> = {
                    call.respondRedirect(UrlPaths.top, false)
                }
                override val twitterCallFailed: OutputInterceptor<TwitterCallFailedException> = {
                    it.printStackTrace()
                    call.respond(HttpStatusCode.BadGateway, "Failed to request for Twitter")
                }
                override val temporaryCredentialNotFound: OutputInterceptor<TemporaryCredentialNotFoundException> = {
                    call.respond(HttpStatusCode.BadRequest, "Temporary credential not found")
                }
            })
    }
}
