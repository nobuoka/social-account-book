@file:JvmName("Application")

package info.vividcode.sbs.main

import info.vividcode.ktor.twitter.login.*
import info.vividcode.ktor.twitter.login.application.TemporaryCredentialNotFoundException
import info.vividcode.ktor.twitter.login.application.TwitterCallFailedException
import info.vividcode.oauth.OAuth
import io.ktor.application.Application
import io.ktor.application.ApplicationCallPipeline
import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.response.respondRedirect
import io.ktor.response.respondText
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
        get(UrlPaths.top) {
            call.respondText("Hello world!", ContentType.Text.Plain)
        }

        get(UrlPaths.login) {
            val loginPageHtml = """
                <!DOCTYPE html>
                <html>
                  <head><title>Login</title></head>
                  <body>
                    <h1>Login</h1>
                    <form method="POST" action="${UrlPaths.TwitterLogin.start}">
                      <button>Sign in with Twitter</button>
                    </form>
                  </body>
                </html>
            """.trimIndent()
            call.respondText(loginPageHtml, ContentType.Text.Html.withParameter("charset", "utf-8"))
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
