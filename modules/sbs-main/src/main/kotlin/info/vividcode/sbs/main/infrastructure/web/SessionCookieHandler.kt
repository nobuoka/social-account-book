package info.vividcode.sbs.main.infrastructure.web

import info.vividcode.sbs.main.auth.domain.SessionId
import io.ktor.http.Cookie
import io.ktor.request.ApplicationRequest
import io.ktor.response.ApplicationResponse
import io.ktor.util.date.GMTDate

internal class SessionCookieHandler(
    private val cookieCodec: CookieCodec<SessionId>,
    private val sessionCookieName: String = "sbs-session"
) {

    internal fun appendCookieSessionId(response: ApplicationResponse, sessionId: SessionId) {
        response.cookies.append(
            Cookie(
                sessionCookieName,
                cookieCodec.encode(sessionId),
                maxAge = 7 * 24 * 60 * 60,
                path = "/"
            )
        )
    }

    internal fun clearCookieSessionId(response: ApplicationResponse) {
        response.cookies.append(
            Cookie(sessionCookieName, "", expires = GMTDate.START, path = "/")
        )
    }

    internal fun getCookieSessionIdOrNull(request: ApplicationRequest): SessionId? =
        request.cookies[sessionCookieName]?.let(cookieCodec::decode)

}
