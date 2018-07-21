package info.vividcode.sbs.main.infrastructure.web

import io.ktor.http.Cookie
import io.ktor.request.ApplicationRequest
import io.ktor.response.ApplicationResponse
import java.time.Instant

private const val sessionCookieName = "sbs-session"

fun ApplicationResponse.appendCookieSessionId(sessionId: Long) {
    cookies.append(
        Cookie(sessionCookieName, "$sessionId", maxAge = 7 * 24 * 60 * 60, path = "/")
    )
}

fun ApplicationResponse.clearCookieSessionId() {
    cookies.append(Cookie(sessionCookieName, "", expires = Instant.EPOCH, path = "/"))
}

fun ApplicationRequest.getCookieSessionIdOrNull() = call.request.cookies[sessionCookieName]?.toLongOrNull()
