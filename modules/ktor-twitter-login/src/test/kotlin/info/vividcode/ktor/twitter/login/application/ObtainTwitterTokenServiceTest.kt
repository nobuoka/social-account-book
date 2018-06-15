package info.vividcode.ktor.twitter.login.application

import info.vividcode.ktor.twitter.login.ClientCredential
import info.vividcode.ktor.twitter.login.TemporaryCredential
import info.vividcode.ktor.twitter.login.TemporaryCredentialStore
import info.vividcode.ktor.twitter.login.TwitterToken
import info.vividcode.ktor.twitter.login.test.TestCallFactory
import info.vividcode.oauth.OAuth
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.newSingleThreadContext
import kotlinx.coroutines.experimental.runBlocking
import okhttp3.*
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.coroutines.experimental.CoroutineContext

internal class ObtainTwitterTokenServiceTest {

    private val testServerTwitter = TestServerTwitter()
    private val testTemporaryCredentialStore = object : TemporaryCredentialStore {
        private val map: MutableMap<String, TemporaryCredential> = ConcurrentHashMap()

        override suspend fun saveTemporaryCredential(temporaryCredential: TemporaryCredential) {
            delay(1, TimeUnit.NANOSECONDS)
            map[temporaryCredential.token] = temporaryCredential
        }

        override suspend fun findTemporaryCredential(token: String): TemporaryCredential? {
            delay(1, TimeUnit.NANOSECONDS)
            return map[token]
        }
    }

    private val testTarget = ObtainTwitterTokenService(object : ObtainTwitterTokenService.Required {
        override val temporaryCredentialStore: TemporaryCredentialStore = testTemporaryCredentialStore
        override val oauth: OAuth = OAuth(object : OAuth.Env {
            override val clock: Clock = Clock.fixed(Instant.parse("2010-01-01T00:00:00Z"), ZoneId.systemDefault())
            override val nextInt: (Int) -> Int = { (it - 1) / 2 }
        })
        override val httpClient: Call.Factory = TestCallFactory(testServerTwitter)
        override val httpCallContext: CoroutineContext = newSingleThreadContext("TestHttpCall")
    })

    private val testClientCredential = ClientCredential("test-identifier", "test-shared-secret")

    @Test
    fun obtainRedirectUrl() {
        runBlocking {
            testTemporaryCredentialStore.saveTemporaryCredential(TemporaryCredential("test-token", "test-secret"))
        }
        testServerTwitter.responseBuilders.add(responseBuilder {
            code(200).message("OK")
            body(
                ResponseBody.create(
                    MediaType.parse("application/x-www-form-urlencoded"),
                    "oauth_token=test-access-token&oauth_token_secret=test-token-secret&user_id=101&screen_name=foo"
                )
            )
        })

        // Act
        val twitterToken = runBlocking {
            testTarget.obtainTwitterToken(testClientCredential, "test-token", "test-verifier")
        }

        // Assert
        Assertions.assertEquals(TwitterToken("test-access-token", "test-token-secret", "101", "foo"), twitterToken)
    }

    @Test
    fun obtainRedirectUrl_unknownToken() {
        // Act and Assert
        Assertions.assertThrows(TemporaryCredentialNotFoundException::class.java, {
            runBlocking {
                testTarget.obtainTwitterToken(testClientCredential, "unknown-token", "test-verifier")
            }
        })
    }

    @Test
    fun obtainRedirectUrl_unexpectedResponseContent_nullBody() {
        runBlocking {
            testTemporaryCredentialStore.saveTemporaryCredential(TemporaryCredential("test-token", "test-secret"))
        }
        testServerTwitter.responseBuilders.add(responseBuilder {
            code(200).message("OK")
        })

        // Act and Assert
        Assertions.assertThrows(TwitterCallFailedException::class.java, {
            runBlocking {
                testTarget.obtainTwitterToken(testClientCredential, "test-token", "test-verifier")
            }
        })
    }

    @Nested
    inner class MissProperty {
        open inner class MissPropertyTestCase(private val responseBody: String) {
            @Test
            fun obtainRedirectUrl_unexpectedResponseContent_missProperty() {
                runBlocking {
                    testTemporaryCredentialStore.saveTemporaryCredential(TemporaryCredential("test-token", "test-secret"))
                }
                testServerTwitter.responseBuilders.add(responseBuilder {
                    code(200).message("OK")
                    body(
                        ResponseBody.create(
                            MediaType.parse("application/x-www-form-urlencoded"),
                            responseBody
                        )
                    )
                })

                // Act and Assert
                Assertions.assertThrows(TwitterCallFailedException::class.java, {
                    runBlocking {
                        testTarget.obtainTwitterToken(testClientCredential, "test-token", "test-verifier")
                    }
                })
            }
        }

        @Nested
        inner class MissPropertyOAuthToken :
            MissPropertyTestCase("foo=test-access-token&oauth_token_secret=test-token-secret&user_id=101&screen_name=foo")

        @Nested
        inner class MissPropertyOAuthTokenSecret :
            MissPropertyTestCase("oauth_token=test-access-token&foo=test-token-secret&user_id=101&screen_name=foo")

        @Nested
        inner class MissPropertyUserId :
            MissPropertyTestCase("oauth_token=test-access-token&oauth_token_secret=test-token-secret&foo=101&screen_name=foo")

        @Nested
        inner class MissPropertyScreenName :
            MissPropertyTestCase("oauth_token=test-access-token&oauth_token_secret=test-token-secret&user_id=101&foo=foo")
    }

    @Test
    fun obtainRedirectUrl_errorResponse() {
        runBlocking {
            testTemporaryCredentialStore.saveTemporaryCredential(TemporaryCredential("test-token", "test-secret"))
        }
        testServerTwitter.responseBuilders.add(responseBuilder {
            code(500).message("Internal Server Error")
        })

        // Act and Assert
        Assertions.assertThrows(TwitterCallFailedException::class.java, {
            runBlocking {
                testTarget.obtainTwitterToken(testClientCredential, "test-token", "test-verifier")
            }
        })
    }

    @Test
    fun obtainRedirectUrl_callFailed() {
        runBlocking {
            testTemporaryCredentialStore.saveTemporaryCredential(TemporaryCredential("test-token", "test-secret"))
        }
        testServerTwitter.responseBuilders.add({
            throw IOException("Test exception")
        })

        // Act and Assert
        Assertions.assertThrows(TwitterCallFailedException::class.java, {
            runBlocking {
                testTarget.obtainTwitterToken(testClientCredential, "test-token", "test-verifier")
            }
        })
    }


    companion object {
        private fun responseBuilder(builder: Response.Builder.() -> Unit): (Request) -> Response = {
            Response.Builder().request(it).protocol(Protocol.HTTP_1_1).also(builder).build()
        }
    }

}
