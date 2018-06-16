package info.vividcode.ktor.twitter.login

import info.vividcode.ktor.twitter.login.application.TemporaryCredentialNotFoundException
import info.vividcode.ktor.twitter.login.application.TestServerTwitter
import info.vividcode.ktor.twitter.login.application.TwitterCallFailedException
import info.vividcode.ktor.twitter.login.application.responseBuilder
import info.vividcode.ktor.twitter.login.test.TestCallFactory
import info.vividcode.ktor.twitter.login.test.TestTemporaryCredentialStore
import info.vividcode.oauth.OAuth
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.response.respondText
import io.ktor.routing.routing
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.withTestApplication
import kotlinx.coroutines.experimental.newSingleThreadContext
import okhttp3.Call
import okhttp3.MediaType
import okhttp3.ResponseBody
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlin.coroutines.experimental.CoroutineContext

internal class RoutingTest {

    private val testServer = TestServerTwitter()
    private val testTemporaryCredentialStore = TestTemporaryCredentialStore()

    private val testEnv = object : Env {
        override val oauth: OAuth = OAuth(object : OAuth.Env {
            override val clock: Clock = Clock.fixed(Instant.parse("2015-01-01T00:00:00Z"), ZoneId.systemDefault())
            override val nextInt: (Int) -> Int = { (it - 1) / 2 }
        })
        override val httpClient: Call.Factory = TestCallFactory(testServer)
        override val httpCallContext: CoroutineContext = newSingleThreadContext("TestHttpCall")
        override val temporaryCredentialStore: TemporaryCredentialStore = testTemporaryCredentialStore
    }

    private val outputPort = object : OutputPort {
        override val success: OutputInterceptor<TwitterToken> = {
            call.respondText("test success")
        }
        override val twitterCallFailed: OutputInterceptor<TwitterCallFailedException> = {
            call.respondText("test twitter call failed")
        }
        override val temporaryCredentialNotFound: OutputInterceptor<TemporaryCredentialNotFoundException> = {
            call.respondText("test temporary credential not found (identifier : ${it.temporaryCredentialIdentifier})")
        }
    }

    private val testableModule: Application.() -> Unit = {
        routing {
            setupTwitterLogin("/start", "/callback", "http://example.com",
                ClientCredential("test-identifier", "test-secret"), testEnv, outputPort)
        }
    }

    @Test
    fun testRequest() = withTestApplication(testableModule) {
        testServer.responseBuilders.add(responseBuilder {
            code(200).message("OK")
            body(
                ResponseBody.create(
                    MediaType.parse("application/x-www-form-urlencoded"),
                    "oauth_token=test-temporary-token&oauth_token_secret=test-token-secret"
                )
            )
        })

        handleRequest(HttpMethod.Post, "/start").run {
            assertEquals(HttpStatusCode.Found, response.status())
            assertEquals(
                "https://api.twitter.com/oauth/authenticate?oauth_token=test-temporary-token",
                response.headers["Location"]
            )
        }

        testServer.responseBuilders.add(responseBuilder {
            code(200).message("OK")
            body(
                ResponseBody.create(
                    MediaType.parse("application/x-www-form-urlencoded"),
                    "oauth_token=test-access-token&oauth_token_secret=test-token-secret&user_id=101&screen_name=foo"
                )
            )
        })

        handleRequest(HttpMethod.Get, "/callback?oauth_token=test-temporary-token&verifier=test-verifier").run {
            assertEquals(HttpStatusCode.OK, response.status())
            assertEquals("test success", response.content)
        }
    }

}
