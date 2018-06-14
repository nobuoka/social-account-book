package info.vividcode.ktor.twitter.login.test

import okhttp3.Call
import okhttp3.Callback
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future

class TestCall(
    private val request: Request,
    private val testServer: TestServer,
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()
) : Call {

    override fun request(): Request = request
    override fun clone(): Call = TestCall(request, testServer)

    private var executed = false
    private var cancelled = false
    private var requestCancel: (() -> Unit)? = null

    override fun execute(): Response {
        if (executed) throw IllegalStateException("Already executed")
        executed = true

        requestCancel = ThreadCanceller(Thread.currentThread())
        return executeCancellable()
    }

    override fun enqueue(responseCallback: Callback) {
        if (executed) throw IllegalStateException("Already executed")
        executed = true

        val task = EnqueuedTask(responseCallback)
        val future = executor.submit(task)
        requestCancel = FutureCanceller(task, future)
    }

    private inner class EnqueuedTask(val responseCallback: Callback) : Runnable {
        var started: Boolean = false
        override fun run() {
            synchronized(this) {
                started = true
            }
            if (Thread.interrupted()) {
                throw InterruptedException()
            }
            try {
                val response = executeCancellable()
                responseCallback.onResponse(this@TestCall, response)
            } catch (e: IOException) {
                responseCallback.onFailure(this@TestCall, e)
            }
        }
    }

    private fun executeCancellable(): Response = try {
        testServer.onReceive(request)
    } catch (e: InterruptedException) {
        throw IOException(e)
    }

    override fun isExecuted(): Boolean = executed

    override fun isCanceled(): Boolean = cancelled
    override fun cancel() {
        cancelled = true
        requestCancel?.invoke()
        requestCancel = null
    }

    private class ThreadCanceller(val t: Thread) : () -> Unit {
        override fun invoke() {
            t.interrupt()
        }
    }

    private inner class FutureCanceller(val task: EnqueuedTask, val f: Future<*>) : () -> Unit {
        override fun invoke() {
            f.cancel(true)
            synchronized(task) {
                if (!task.started) {
                    task.responseCallback.onFailure(this@TestCall, IOException("Cancelled", null))
                }
            }
        }
    }

}
