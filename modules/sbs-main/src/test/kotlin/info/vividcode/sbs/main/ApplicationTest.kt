package info.vividcode.sbs.main

import info.vividcode.orm.OrmContextProvider
import info.vividcode.orm.TransactionManager
import info.vividcode.sbs.main.auth.domain.Session
import info.vividcode.sbs.main.auth.domain.SessionId
import info.vividcode.sbs.main.auth.domain.createSession
import info.vividcode.sbs.main.core.domain.AccountBook
import info.vividcode.sbs.main.core.domain.User
import info.vividcode.sbs.main.core.domain.createUser
import info.vividcode.sbs.main.core.domain.createUserAccountBook
import info.vividcode.sbs.main.infrastructure.database.AppOrmContext
import info.vividcode.sbs.main.infrastructure.database.createTransactionManager
import info.vividcode.sbs.main.infrastructure.web.SessionCookieEncrypt
import io.ktor.config.MapApplicationConfig
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.*
import kotlinx.coroutines.runBlocking
import org.h2.jdbcx.JdbcDataSource
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

internal class ApplicationTest {

    companion object {
        private const val cookieHeaderName = "Cookie"
        private const val testDbJdbcUrl = "jdbc:h2:mem:sbs_dev;TRACE_LEVEL_FILE=4"
        private const val sessionCookieName = "sbs-session"
        private const val sessionCookieEncryptionKey = "0123456789ABCDEF"
        private const val sessionCookieSignKey = "01234567"

        private val contentTypeTextHtmlUtf8 = ContentType.parse("text/html; charset=utf-8")
    }

    private val dataSource = JdbcDataSource().apply { setUrl(testDbJdbcUrl) }
    private val txManager = createTransactionManager(dataSource)

    private val sessionCookieCodec =
        SessionCookieEncrypt(sessionCookieEncryptionKey.toByteArray(), sessionCookieSignKey.toByteArray())
            .createCodec()

    private fun withSbsTestApplication(test: TestApplicationEngine.() -> Unit): Unit = withTestApplication({
        (environment.config as MapApplicationConfig).apply {
            put("sbs.contextUrl", "http://test.example.com")
            put("sbs.session.encryptionKey", sessionCookieEncryptionKey)
            put("sbs.session.signKey", sessionCookieSignKey)
            put("sbs.twitter.clientCredential.identifier", "test-twitter-client-identifier")
            put("sbs.twitter.clientCredential.sharedSecret", "test-twitter-client-shared-secret")
        }
        setup()
    }, test)

    @Test
    internal fun setupApplication(): Unit = withSbsTestApplication {
        // Do nothing.
    }

    @Test
    internal fun getTop_notLoggedIn(): Unit = withSbsTestApplication {
        with(handleRequest(HttpMethod.Get, "/")) {
            Assertions.assertTrue(requestHandled)
            Assertions.assertEquals(HttpStatusCode.OK, response.status())
            Assertions.assertEquals(contentTypeTextHtmlUtf8, response.contentType())
        }
    }

    @Test
    internal fun getTop_loggedIn(): Unit = withSbsTestApplication {
        val session = createUserAndSession()
        with(handleRequest(HttpMethod.Get, "/") { setSessionCookie(session.id) }) {
            Assertions.assertTrue(requestHandled)
            Assertions.assertEquals(HttpStatusCode.OK, response.status())
            Assertions.assertEquals(contentTypeTextHtmlUtf8, response.contentType())
        }
    }

    @Test
    internal fun getUserPrivateHome_notLoggedIn(): Unit = withSbsTestApplication {
        with(handleRequest(HttpMethod.Get, "/-/up/1")) {
            Assertions.assertFalse(requestHandled)
        }
    }

    @Test
    internal fun getUserPrivateHome_loggedIn(): Unit = withSbsTestApplication {
        val session = createUserAndSession()
        with(handleRequest(HttpMethod.Get, "/-/up/${session.user.id}") { setSessionCookie(session.id) }) {
            Assertions.assertTrue(requestHandled)
            Assertions.assertEquals(HttpStatusCode.OK, response.status())
            Assertions.assertEquals(contentTypeTextHtmlUtf8, response.contentType())
        }
    }

    @Nested
    internal inner class UserAccountBooksTest {
        @Test
        internal fun getUserAccountBooks_notLoggedIn(): Unit = withSbsTestApplication {
            with(handleRequest(HttpMethod.Get, "/-/up/1/account-books/1")) {
                Assertions.assertFalse(requestHandled)
            }
        }

        @Test
        internal fun getUserAccountBooks_loggedIn_exist(): Unit = withSbsTestApplication {
            val session = createUserAndSession()
            val accountBook = createUserAccountBook(session.user)
            with(handleRequest(HttpMethod.Get, "/-/up/${session.user.id}/account-books/${accountBook.id}") {
                setSessionCookie(session.id)
            }) {
                Assertions.assertTrue(requestHandled)
                Assertions.assertEquals(HttpStatusCode.OK, response.status())
                Assertions.assertEquals(contentTypeTextHtmlUtf8, response.contentType())
            }
        }

        @Test
        internal fun getUserAccountBooks_loggedIn_invalidPathParameters(): Unit = withSbsTestApplication {
            val session = createUserAndSession()
            with(handleRequest(HttpMethod.Get, "/-/up/invalid-user-id/account-books/invalid-account-book-id") {
                setSessionCookie(session.id)
            }) {
                Assertions.assertFalse(requestHandled)
            }
        }

        @Test
        internal fun postUserAccountBooks_loggedIn_noLabel(): Unit = withSbsTestApplication {
            val session = createUserAndSession()
            with(handleRequest(HttpMethod.Post, "/-/up/${session.user.id}/account-books") {
                setSessionCookie(session.id)
            }) {
                Assertions.assertTrue(requestHandled)
                Assertions.assertEquals(HttpStatusCode.Found, response.status())
                Assertions.assertEquals(listOf("/-/up/${session.user.id}"), response.headers.values("Location"))
            }
        }

        @Test
        internal fun postUserAccountBooks_loggedIn_withLabel(): Unit = withSbsTestApplication {
            val session = createUserAndSession()
            with(handleRequest(HttpMethod.Post, "/-/up/${session.user.id}/account-books") {
                setSessionCookie(session.id)
                addHeader("Content-Type", "application/x-www-form-urlencoded")
                setBody("label=Test+Account+Book")
            }) {
                Assertions.assertTrue(requestHandled)
                Assertions.assertEquals(HttpStatusCode.Found, response.status())
                Assertions.assertEquals(listOf("/-/up/${session.user.id}"), response.headers.values("Location"))
            }
        }

        @Test
        internal fun postUserAccountBooks_invalidPathParameters(): Unit = withSbsTestApplication {
            val session = createUserAndSession()
            with(handleRequest(HttpMethod.Post, "/-/up/invalid-user-id/account-books") {
                setSessionCookie(session.id)
                addHeader("Content-Type", "application/x-www-form-urlencoded")
                setBody("label=Test+Account+Book")
            }) {
                Assertions.assertFalse(requestHandled)
            }
        }

        @Test
        internal fun postUserAccountBooks_notLoggedIn(): Unit = withSbsTestApplication {
            with(handleRequest(HttpMethod.Post, "/-/up/1/account-books")) {
                Assertions.assertFalse(requestHandled)
            }
        }
    }

    @Nested
    internal inner class UserAccountsTest {
        @Test
        internal fun postUserAccounts_notLoggedIn(): Unit = withSbsTestApplication {
            with(handleRequest(HttpMethod.Post, "/-/up/1/accounts")) {
                Assertions.assertFalse(requestHandled)
            }
        }

        @Test
        internal fun postUserAccounts_loggedIn_noLabel(): Unit = withSbsTestApplication {
            val session = createUserAndSession()
            val accountBook = createUserAccountBook(session.user)
            with(handleRequest(HttpMethod.Post, "/-/up/${session.user.id}/accounts") {
                setSessionCookie(session.id)
                addHeader("Content-Type", "application/x-www-form-urlencoded")
                setBody("account-book-id=${accountBook.id}")
            }) {
                Assertions.assertTrue(requestHandled)
                Assertions.assertEquals(HttpStatusCode.Found, response.status())
            }
        }

        @Test
        internal fun postUserAccounts_loggedIn_withLabel(): Unit = withSbsTestApplication {
            val session = createUserAndSession()
            val accountBook = createUserAccountBook(session.user)
            with(handleRequest(HttpMethod.Post, "/-/up/${session.user.id}/accounts") {
                setSessionCookie(session.id)
                addHeader("Content-Type", "application/x-www-form-urlencoded")
                setBody("account-book-id=${accountBook.id}&label=Test+Bank")
            }) {
                Assertions.assertTrue(requestHandled)
                Assertions.assertEquals(HttpStatusCode.Found, response.status())
            }
        }

        @Test
        internal fun postUserAccounts_loggedIn_invalidAccountBookId(): Unit = withSbsTestApplication {
            val session = createUserAndSession()
            val exception = Assertions.assertThrows(RuntimeException::class.java) {
                handleRequest(HttpMethod.Post, "/-/up/${session.user.id}/accounts") {
                    setSessionCookie(session.id)
                    addHeader("Content-Type", "application/x-www-form-urlencoded")
                    setBody("account-book-id=invalid")
                }
            }
            Assertions.assertEquals("account-book-id not specified", exception.message)
        }

        @Test
        internal fun postUserAccounts_loggedIn_invalidRequestBody(): Unit = withSbsTestApplication {
            val session = createUserAndSession()
            val exception = Assertions.assertThrows(RuntimeException::class.java) {
                handleRequest(HttpMethod.Post, "/-/up/${session.user.id}/accounts") {
                    setSessionCookie(session.id)
                    addHeader("Content-Type", "text/plain")
                    setBody("invalid")
                }
            }
            Assertions.assertEquals("account-book-id not specified", exception.message)
        }

        @Test
        internal fun postUserAccounts_loggedIn_invalidPathParameters(): Unit = withSbsTestApplication {
            val session = createUserAndSession()
            val accountBook = createUserAccountBook(session.user)
            with(handleRequest(HttpMethod.Post, "/-/up/invalid-user-id/accounts") {
                    setSessionCookie(session.id)
                    addHeader("Content-Type", "application/x-www-form-urlencoded")
                    setBody("account-book-id=${accountBook.id}")
            }) {
                Assertions.assertFalse(requestHandled)
            }
        }
    }

    private suspend fun <R> TransactionManager<OrmContextProvider<AppOrmContext>>.withOrmContext(execution: AppOrmContext.() -> R) =
        withTransaction { it.withOrmContext(execution) }

    private fun createUserAndSession(): Session = runBlocking {
        txManager.withOrmContext {
            val user = createUser("test-user")
            val sessionId = createSession(user)
            Session(sessionId, user)
        }
    }

    private fun createUserAccountBook(targetUser: User): AccountBook = runBlocking {
        txManager.withOrmContext {
            createUserAccountBook(targetUser, "test-user")
        }
    }

    private fun TestApplicationRequest.setSessionCookie(sessionId: SessionId) =
        addHeader(
            cookieHeaderName,
            "$sessionCookieName=${sessionCookieCodec.encode(sessionId)}"
        )

}
