package com.karthi.sungmobile.core.youtube

import com.karthi.sungmobile.core.model.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull
import org.schabi.newpipe.extractor.exceptions.*
import java.io.Closeable
import java.io.IOException

internal class ExtractorExecution(threads: Int) : Closeable {
    private val runner = ExtractorRunner(threads)
    suspend fun <T> run(block: () -> T): T = try {
        // Limit the entire extraction, which can make several individual HTTP requests.
        val result = withTimeoutOrNull(60_000) { Result.success(runner.run(block)) }
            ?: throw SourceException(SourceFailure.NETWORK)
        result.getOrThrow()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: SourceException) {
        throw error
    } catch (error: Exception) {
        val failure = when (error) {
            is ReCaptchaException -> SourceFailure.AUTHENTICATION_REQUIRED
            is ContentNotAvailableException -> SourceFailure.UNAVAILABLE
            is HttpStatusException -> SourceFailure.RATE_LIMITED
            is IOException -> SourceFailure.NETWORK
            else -> SourceFailure.EXTRACTION
        }
        throw SourceException(failure, error)
    }

    override fun close() = runner.close()
}
