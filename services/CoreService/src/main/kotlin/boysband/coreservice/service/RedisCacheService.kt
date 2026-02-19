package boysband.coreservice.service

import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.ZonedDateTime

/**
 * Redis-backed cache for CoreService.
 * Replaces in-memory ConcurrentHashMap for lastNotifiedDate tracking.
 */
@Service
class RedisCacheService(
    private val redisTemplate: StringRedisTemplate
) {

    companion object {
        private const val PREFIX = "core:cache:"
        private val TTL: Duration = Duration.ofDays(3)
    }

    // ── Last Notified Date (per actionId) ────────────────────────────────────

    fun getLastNotifiedDate(actionId: Int): ZonedDateTime? {
        val value = redisTemplate.opsForValue().get("${PREFIX}lastNotified:$actionId") ?: return null
        return try {
            ZonedDateTime.parse(value)
        } catch (_: Exception) {
            null
        }
    }

    fun setLastNotifiedDate(actionId: Int, date: ZonedDateTime) {
        redisTemplate.opsForValue().set(
            "${PREFIX}lastNotified:$actionId",
            date.toString(),
            TTL
        )
    }

    // ── Generic key-value cache ──────────────────────────────────────────────

    fun get(key: String): String? =
        redisTemplate.opsForValue().get("${PREFIX}$key")

    fun set(key: String, value: String, ttl: Duration = Duration.ofMinutes(2)) {
        redisTemplate.opsForValue().set("${PREFIX}$key", value, ttl)
    }

    fun delete(key: String) {
        redisTemplate.delete("${PREFIX}$key")
    }
}
