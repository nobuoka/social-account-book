package info.vividcode.ktor.twitter.login.application

import info.vividcode.ktor.twitter.login.ClientCredential
import info.vividcode.ktor.twitter.login.TemporaryCredentialStore
import info.vividcode.ktor.twitter.login.test.TestCallFactory
import info.vividcode.oauth.OAuth
import kotlinx.coroutines.experimental.newSingleThreadContext
import kotlinx.coroutines.experimental.runBlocking
import okhttp3.Call
import okhttp3.MediaType
import okhttp3.ResponseBody
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlin.coroutines.experimental.CoroutineContext

internal class ObtainRedirectUrlServiceTest {

    private val testServerTwitter = TestServerTwitter()

    private val testTarget = ObtainRedirectUrlService(object : ObtainRedirectUrlService.Required {
        override val temporaryCredentialStore: TemporaryCredentialStore =
            TemporaryCredentialStore.OnMemoryTemporaryCredentialStore
        override val oauth: OAuth = OAuth(object : OAuth.Env {
            override val clock: Clock = Clock.fixed(Instant.parse("2010-01-01T00:00:00Z"), ZoneId.systemDefault())
            override val nextInt: (Int) -> Int = { (it - 1) / 2 }
        })
        override val httpClient: Call.Factory = TestCallFactory(testServerTwitter)
        override val httpCallContext: CoroutineContext = newSingleThreadContext("TestHttpCall")
    })

    @Test
    fun obtainRedirectUrl() {
        val testClientCredential =
            ClientCredential("test-identifier", "test-shared-secret")

        testServerTwitter.responseBuilders.add(responseBuilder {
            code(200).message("OK")
            body(
                ResponseBody.create(
                    MediaType.parse("application/x-www-form-urlencoded"),
                    "oauth_token=dd&oauth_token_secret=a"
                )
            )
        })

        // Act
        val redirectUrl = runBlocking {
            testTarget.obtainRedirectUrl(testClientCredential, "http://example.com/callback")
        }

        // Assert
        Assertions.assertEquals("https://api.twitter.com/oauth/authenticate?oauth_token=dd", redirectUrl)
    }

    @Test
    fun obtainRedirectUrl_unexpectedResponseContent_nullBody() {
        val testClientCredential =
            ClientCredential("test-identifier", "test-shared-secret")

        testServerTwitter.responseBuilders.add(responseBuilder {
            code(200).message("OK")
        })

        // Act and Assert
        Assertions.assertThrows(TwitterCallFailedException::class.java, {
            runBlocking {
                testTarget.obtainRedirectUrl(testClientCredential, "http://example.com/callback")
            }
        })
    }

    @Test
    fun obtainRedirectUrl_unexpectedResponseContent_missProperty_oauthTokenSecret() {
        val testClientCredential =
            ClientCredential("test-identifier", "test-shared-secret")

        testServerTwitter.responseBuilders.add(responseBuilder {
            code(200).message("OK")
            body(
                ResponseBody.create(
                    MediaType.parse("application/x-www-form-urlencoded"),
                    "oauth_token=dd&foo=a"
                )
            )
        })

        // Act and Assert
        Assertions.assertThrows(TwitterCallFailedException::class.java, {
            runBlocking {
                testTarget.obtainRedirectUrl(testClientCredential, "http://example.com/callback")
            }
        })
    }

    @Test
    fun obtainRedirectUrl_unexpectedResponseContent_missProperty_oauthToken() {
        val testClientCredential =
            ClientCredential("test-identifier", "test-shared-secret")

        testServerTwitter.responseBuilders.add(responseBuilder {
            code(200).message("OK")
            body(
                ResponseBody.create(
                    MediaType.parse("application/x-www-form-urlencoded"),
                    "foo=dd&oauth_token_secret=a"
                )
            )
        })

        // Act and Assert
        Assertions.assertThrows(TwitterCallFailedException::class.java, {
            runBlocking {
                testTarget.obtainRedirectUrl(testClientCredential, "http://example.com/callback")
            }
        })
    }

    @Test
    fun obtainRedirectUrl_errorResponse() {
        val testClientCredential =
            ClientCredential("test-identifier", "test-shared-secret")

        testServerTwitter.responseBuilders.add(responseBuilder {
            code(500).message("Internal Server Error")
        })

        // Act and Assert
        Assertions.assertThrows(TwitterCallFailedException::class.java, {
            runBlocking {
                testTarget.obtainRedirectUrl(testClientCredential, "http://example.com/callback")
            }
        })
    }

    @Test
    fun obtainRedirectUrl_callFailed() {
        val testClientCredential =
            ClientCredential("test-identifier", "test-shared-secret")

        testServerTwitter.responseBuilders.add({
            throw IOException("Test exception")
        })

        // Act and Assert
        Assertions.assertThrows(TwitterCallFailedException::class.java, {
            runBlocking {
                testTarget.obtainRedirectUrl(testClientCredential, "http://example.com/callback")
            }
        })
    }

}
