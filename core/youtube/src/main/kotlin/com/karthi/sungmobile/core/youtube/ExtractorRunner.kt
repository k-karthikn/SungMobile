package com.karthi.sungmobile.core.youtube

import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import java.io.Closeable
import java.io.InterruptedIOException
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** NewPipe is synchronous. Each operation owns its calls, so cancellation closes sockets too. */
internal class ExtractorRunner(threads: Int = 3) : Closeable {
    private val executor = ThreadPoolExecutor(threads, threads, 0, TimeUnit.MILLISECONDS, LinkedBlockingQueue(24)) { task ->
        Thread(task, "sung-extractor").apply { isDaemon = true }
    }

    suspend fun <T> run(block: () -> T): T = suspendCancellableCoroutine { continuation ->
        val operation = HttpOperation()
        val future = executor.submit {
            HttpOperation.current.set(operation)
            try {
                operation.ensureActive()
                val result = block()
                continuation.resume(result)
            } catch (error: Exception) {
                continuation.resumeWithException(error)
            } finally {
                HttpOperation.current.remove()
            }
        }
        continuation.invokeOnCancellation {
            operation.cancel()
            future.cancel(true)
            executor.purge()
        }
    }

    override fun close() { executor.shutdownNow() }
}

internal class HttpOperation {
    private var cancelled = false
    private val calls = mutableSetOf<Call>()

    @Synchronized fun ensureActive() {
        if (cancelled || Thread.currentThread().isInterrupted) throw InterruptedIOException()
    }

    @Synchronized fun attach(call: Call) {
        ensureActive()
        calls += call
    }

    @Synchronized fun detach(call: Call) { calls -= call }

    @Synchronized fun cancel() {
        cancelled = true
        calls.forEach(Call::cancel)
    }

    companion object { val current = ThreadLocal<HttpOperation>() }
}
