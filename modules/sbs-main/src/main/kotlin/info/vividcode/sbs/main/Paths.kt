package info.vividcode.sbs.main

import info.vividcode.sbs.main.core.domain.User
import io.ktor.application.ApplicationCall
import io.ktor.request.ApplicationRequest
import io.ktor.request.path

object UrlPaths {

    const val top = "/"

    const val logout = "/auth/logout"

    internal object UserPrivate {
        internal object Home {
            const val parameterized = "/-/up/{userId}"
            internal fun concrete(user: User) = "/-/up/${user.id}"
            internal fun getUserId(request: ApplicationCall) = request.parameters["userId"]
        }

        internal object Accounts {
            const val parameterized = "/-/up/{userId}/accounts"
            internal fun concrete(user: User) = "/-/up/${user.id}/accounts"
            internal fun getUserId(request: ApplicationCall) = request.parameters["userId"]
        }
    }

    object TwitterLogin {
        const val start: String = "/auth/twitter/login"
        const val callback: String = "/auth/twitter/callback"
    }

}
