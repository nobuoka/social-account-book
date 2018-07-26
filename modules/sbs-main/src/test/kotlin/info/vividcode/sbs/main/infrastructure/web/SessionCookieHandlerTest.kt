package info.vividcode.sbs.main.infrastructure.web

import info.vividcode.sbs.main.auth.domain.SessionId
import io.ktor.application.Application
import io.ktor.application.ApplicationCall
import io.ktor.application.ApplicationCallPipeline
import io.ktor.application.call
import io.ktor.http.HttpMethod
import io.ktor.pipeline.PipelineInterceptor
import io.ktor.response.header
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.withTestApplication
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class SessionCookieHandlerTest {

    private val testSessionCookieName = "test-session"

    private val handler = SessionCookieHandler(
        "0123456789ABCDEF".toByteArray(),
        "01234567".toByteArray(),
        testSessionCookieName
    ) { ByteArray(it) { 0x10 } }

    private val testSessionCookie =
        "test-session=10101010101010101010101010101010%2F683c4d98ebd16578e99d74140cb05fe1" +
                "%3A66026618b373c60b5e67b420751c861834efd14f4f935a584c9dac9d885efca0; " +
                "Max-Age=604800; Path=/; \$x-enc=URI_ENCODING"

    private fun Application.test(interceptor: PipelineInterceptor<Unit, ApplicationCall>) {
        intercept(ApplicationCallPipeline.Call, interceptor)
    }

    @Test
    internal fun appendCookieSessionId(): Unit = withTestApplication({
        test {
            handler.appendCookieSessionId(call.response, SessionId(1001))
        }
    }) {
        with(handleRequest(HttpMethod.Post, "/")) {
            assertEquals(
                listOf(testSessionCookie),
                response.headers.values("Set-Cookie")
            )
        }
    }

    @Test
    internal fun clearCookieSessionId(): Unit = withTestApplication({
        test {
            handler.clearCookieSessionId(call.response)
        }
    }) {
        with(handleRequest(HttpMethod.Post, "/")) {
            assertEquals(
                listOf("test-session=; Expires=Thu, 01 Jan 1970 00:00:00 GMT; Path=/; \$x-enc=URI_ENCODING"),
                response.headers.values("Set-Cookie")
            )
        }
    }

    @Test
    internal fun getCookieSessionIdOrNull(): Unit = withTestApplication({
        test {
            val id = handler.getCookieSessionIdOrNull(call.request)
            call.response.header("Session-Id", "id: ${id?.value}")
        }
    }) {
        with(handleRequest(HttpMethod.Post, "/") {
            addHeader(
                "Cookie",
                "test-session=10101010101010101010101010101010%2F683c4d98ebd16578e99d74140cb05fe1" +
                        "%3A66026618b373c60b5e67b420751c861834efd14f4f935a584c9dac9d885efca0"
            )
        }) {
            assertEquals("id: 1001", response.headers["Session-Id"])
        }

        with(handleRequest(HttpMethod.Post, "/") {
            addHeader(
                "Cookie",
                "test-session=invalid-value"
            )
        }) {
            assertEquals("id: null", response.headers["Session-Id"])
        }

        with(handleRequest(HttpMethod.Post, "/") {
            addHeader(
                "Cookie",
                "test-session=invalid%2Fvalue"
            )
        }) {
            assertEquals("id: null", response.headers["Session-Id"])
        }

        with(handleRequest(HttpMethod.Post, "/") {
            addHeader(
                "Cookie",
                "test-session=10101010101010101010101010101010%2Finvalid%3Avalue"
            )
        }) {
            assertEquals("id: null", response.headers["Session-Id"])
        }

        with(handleRequest(HttpMethod.Post, "/") {
            addHeader(
                "Cookie",
                "test-session=10101010101010101010101010101010%2F683c4d98ebd16578e99d74140cb05fe1%3Avalue"
            )
        }) {
            assertEquals("id: null", response.headers["Session-Id"])
        }

        with(handleRequest(HttpMethod.Post, "/") {
            // No Session Cookie
        }) {
            assertEquals("id: null", response.headers["Session-Id"])
        }
    }

}
