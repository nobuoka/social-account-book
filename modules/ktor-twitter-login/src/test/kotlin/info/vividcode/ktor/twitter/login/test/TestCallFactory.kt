package info.vividcode.ktor.twitter.login.test

import okhttp3.Call
import okhttp3.Request

class TestCallFactory(private val testServer: TestServer) : Call.Factory {

    override fun newCall(request: Request): Call =
        TestCall(request, testServer)

}
