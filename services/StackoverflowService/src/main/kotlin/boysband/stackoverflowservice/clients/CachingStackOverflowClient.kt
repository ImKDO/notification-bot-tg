package boysband.stackoverflowservice.clients

import boysband.stackoverflowservice.dto.Record
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Primary
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.ZonedDateTime

@Component("cachingRealization")
@Primary
class CachingStackOverflowClient(
    @param:Qualifier("retryingRealization")
    private val delegate: StackoverflowClient,
    private val redisTemplate: StringRedisTemplate,
) : StackoverflowClient {

    private val logger = LoggerFactory.getLogger(CachingStackOverflowClient::class.java)

    private val objectMapper = jacksonObjectMapper().apply {
        registerModule(JavaTimeModule())
        disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    }

    companion object {
        private const val PREFIX = "so:cache:"
        private val CACHE_TTL: Duration = Duration.ofSeconds(60)
    }

    override suspend fun searchNewComments(url: String, fromDate: ZonedDateTime?): List<Record> {
        val cacheKey = "${PREFIX}comments:${url}:${fromDate?.toEpochSecond() ?: 0}"

        val cached = getFromCache(cacheKey)
        if (cached != null) {
            logger.debug("Cache HIT for comments: $url")
            return cached
        }

        logger.debug("Cache MISS for comments: $url")
        val result = delegate.searchNewComments(url, fromDate)
        putToCache(cacheKey, result)
        return result
    }

    override suspend fun searchNewAnswers(url: String, fromDate: ZonedDateTime?): List<Record> {
        val cacheKey = "${PREFIX}answers:${url}:${fromDate?.toEpochSecond() ?: 0}"

        val cached = getFromCache(cacheKey)
        if (cached != null) {
            logger.debug("Cache HIT for answers: $url")
            return cached
        }

        logger.debug("Cache MISS for answers: $url")
        val result = delegate.searchNewAnswers(url, fromDate)
        putToCache(cacheKey, result)
        return result
    }

    private fun getFromCache(key: String): List<Record>? {
        return try {
            val json = redisTemplate.opsForValue().get(key) ?: return null
            objectMapper.readValue<List<Record>>(json)
        } catch (e: Exception) {
            logger.warn("Failed to read from Redis cache: ${e.message}")
            null
        }
    }

    private fun putToCache(key: String, records: List<Record>) {
        try {
            val json = objectMapper.writeValueAsString(records)
            redisTemplate.opsForValue().set(key, json, CACHE_TTL)
        } catch (e: Exception) {
            logger.warn("Failed to write to Redis cache: ${e.message}")
        }
    }
}
