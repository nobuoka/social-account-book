package info.vividcode.sbs.main.infrastructure.web

import info.vividcode.sbs.main.auth.domain.SessionId
import io.ktor.http.Cookie
import io.ktor.request.ApplicationRequest
import io.ktor.response.ApplicationResponse
import java.time.Instant

private const val sessionCookieName = "sbs-session"

fun ApplicationResponse.appendCookieSessionId(sessionId: SessionId) {
    cookies.append(
        Cookie(sessionCookieName, "${sessionId.value}", maxAge = 7 * 24 * 60 * 60, path = "/")
    )
}

fun ApplicationResponse.clearCookieSessionId() {
    cookies.append(Cookie(sessionCookieName, "", expires = Instant.EPOCH, path = "/"))
}

fun ApplicationRequest.getCookieSessionIdOrNull(): SessionId? =
    call.request.cookies[sessionCookieName]?.toLongOrNull()?.let(::SessionId)
