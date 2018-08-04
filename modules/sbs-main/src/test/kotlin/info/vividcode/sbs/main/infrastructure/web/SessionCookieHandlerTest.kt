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

    companion object {
        private const val cookieHeaderName = "Cookie"
        private const val setCookieHeaderName = "Set-Cookie"
        private const val testSessionCookieName = "test-session"
        private const val testSessionIdHeaderName = "Session-Id"
    }

    private val handler = SessionCookieHandler(
        SessionCookieEncrypt(
            "0123456789ABCDEF".toByteArray(),
            "01234567".toByteArray()
        ) { ByteArray(it) { 0x10 } }.createCodec(),
        testSessionCookieName
    )

    private val testSessionCookie =
        "test-session=10101010101010101010101010101010%2F683c4d98ebd16578e99d74140cb05fe1" +
                "%3A66026618b373c60b5e67b420751c861834efd14f4f935a584c9dac9d885efca0; " +
                "Max-Age=604800; Path=/; \$x-enc=URI_ENCODING"

    private val testSessionCookieValueByAnotherEncryptKey =
        "10101010101010101010101010101010%2F08d6532aaf7c13685f02878bfe1d530d" +
                "%3A66026618b373c60b5e67b420751c861834efd14f4f935a584c9dac9d885efca0"

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
                response.headers.values(setCookieHeaderName)
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
                response.headers.values(setCookieHeaderName)
            )
        }
    }

    @Test
    internal fun getCookieSessionIdOrNull(): Unit = withTestApplication({
        test {
            val id = handler.getCookieSessionIdOrNull(call.request)
            call.response.header(testSessionIdHeaderName, "id: ${id?.value}")
        }
    }) {
        with(handleRequest(HttpMethod.Post, "/") {
            addHeader(
                cookieHeaderName,
                "test-session=10101010101010101010101010101010%2F683c4d98ebd16578e99d74140cb05fe1" +
                        "%3A66026618b373c60b5e67b420751c861834efd14f4f935a584c9dac9d885efca0"
            )
        }) {
            assertEquals("id: 1001", response.headers[testSessionIdHeaderName])
        }

        val nullSessionId = "id: null"

        with(handleRequest(HttpMethod.Post, "/") {
            addHeader(
                cookieHeaderName,
                "test-session=invalid-value"
            )
        }) {
            assertEquals(nullSessionId, response.headers[testSessionIdHeaderName])
        }

        with(handleRequest(HttpMethod.Post, "/") {
            addHeader(
                cookieHeaderName,
                "test-session=invalid%2Fvalue"
            )
        }) {
            assertEquals(nullSessionId, response.headers[testSessionIdHeaderName])
        }

        with(handleRequest(HttpMethod.Post, "/") {
            addHeader(
                cookieHeaderName,
                "test-session=10101010101010101010101010101010%2Finvalid%3Avalue"
            )
        }) {
            assertEquals(nullSessionId, response.headers[testSessionIdHeaderName])
        }

        with(handleRequest(HttpMethod.Post, "/") {
            addHeader(
                cookieHeaderName,
                "test-session=10101010101010101010101010101010%2F683c4d98ebd16578e99d74140cb05fe1%3Avalue"
            )
        }) {
            assertEquals(nullSessionId, response.headers[testSessionIdHeaderName])
        }

        with(handleRequest(HttpMethod.Post, "/") {
            addHeader(
                cookieHeaderName,
                "test-session=10101010101010101010101010101%2F683c4d98ebd16578e99d74140cb05fe1%3Avalue"
            )
        }) {
            assertEquals(nullSessionId, response.headers[testSessionIdHeaderName])
        }

        with(handleRequest(HttpMethod.Post, "/") {
            addHeader(
                cookieHeaderName,
                "test-session=$testSessionCookieValueByAnotherEncryptKey"
            )
        }) {
            assertEquals(nullSessionId, response.headers[testSessionIdHeaderName])
        }

        with(handleRequest(HttpMethod.Post, "/") {
            // No Session Cookie
        }) {
            assertEquals(nullSessionId, response.headers[testSessionIdHeaderName])
        }
    }

}
