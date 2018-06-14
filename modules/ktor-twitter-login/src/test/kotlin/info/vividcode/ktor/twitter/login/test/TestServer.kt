package info.vividcode.ktor.twitter.login.test

import okhttp3.*

interface TestServer {

    fun onReceive(request: Request): Response

}
