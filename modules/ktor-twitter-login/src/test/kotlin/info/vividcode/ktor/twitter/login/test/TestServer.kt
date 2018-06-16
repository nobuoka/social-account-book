package info.vividcode.ktor.twitter.login.test

import okhttp3.Request
import okhttp3.Response

interface TestServer {

    fun onReceive(request: Request): Response

}
