package boysband.stackoverflowservice.clients

import boysband.stackoverflowservice.dto.Record
import kotlinx.coroutines.delay
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import java.time.ZonedDateTime

@Component("retryingRealization")
class RetryingStackOverflowClient (
    @param:Qualifier("webClientRealization")
    private val delegateClient: StackoverflowClient,
    private val maxRetries: Int = 2,
    private val delayBetweenFetches: Long = 1000L
) : StackoverflowClient{
    override suspend fun searchNewComments(url: String, fromDate: ZonedDateTime?): List<Record> {
        return fetchWithRetry { delegateClient.searchNewComments(url, fromDate) }
    }

    override suspend fun searchNewAnswers(url: String, fromDate: ZonedDateTime?): List<Record> {
        return fetchWithRetry { delegateClient.searchNewAnswers(url, fromDate) }
    }

    //TODO: изменить алгоритм delay?
    private suspend fun <T> fetchWithRetry (fetch: suspend () -> T): T {
        var lastException: Exception? = null

        repeat(maxRetries + 1) {
            try {
                return fetch()
            } catch (e: Exception) {
                lastException = e
                logger.warn("fetch attempt ${it + 1} failed: ${e.message}")
                if (it < maxRetries) {
                    delay(delayBetweenFetches)
                }
            }
        }

        logger.warn("fetch failed")
        throw lastException?: IllegalStateException("fetch failed after $maxRetries retries")
    }

    companion object {
        private val logger = LoggerFactory.getLogger(RetryingStackOverflowClient::class.java)
    }
}