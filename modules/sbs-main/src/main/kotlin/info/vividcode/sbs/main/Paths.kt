package info.vividcode.sbs.main

object UrlPaths {

    const val top = "/"

    const val logout = "/auth/logout"

    object TwitterLogin {
        const val start: String = "/auth/twitter/login"
        const val callback: String = "/auth/twitter/callback"
    }

}
