package info.vividcode.ktor.twitter.login.application

import info.vividcode.ktor.twitter.login.test.TestServer
import okhttp3.Request
import okhttp3.Response
import java.util.concurrent.ConcurrentLinkedQueue

class TestServerTwitter : TestServer {

    val receivedRequests: ConcurrentLinkedQueue<Request> =
        ConcurrentLinkedQueue()
    val responseBuilders: ConcurrentLinkedQueue<(Request) -> Response> =
        ConcurrentLinkedQueue()

    override fun onReceive(request: Request): Response {
        receivedRequests.add(request)
        return responseBuilders.remove().invoke(request)
    }

}
