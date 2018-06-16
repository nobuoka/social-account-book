package info.vividcode.ktor.twitter.login.application

import info.vividcode.ktor.twitter.login.test.TestServer
import okhttp3.Protocol
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
        return try {
            responseBuilders.remove().invoke(request)
        } catch (e: NoSuchElementException) {
            throw RuntimeException("No response builder registered. Is this HTTP request expected one?", e)
        }
    }

}

fun responseBuilder(builder: Response.Builder.() -> Unit): (Request) -> Response = {
    Response.Builder().request(it).protocol(Protocol.HTTP_1_1).also(builder).build()
}
