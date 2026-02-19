import json
import logging
import os
from typing import Any, Optional

from dotenv import load_dotenv

load_dotenv()

REDIS_URL = os.getenv("REDIS_URL", "redis://localhost:6379")

logger = logging.getLogger(__name__)


class RedisCache:
    """Redis cache client for BotService.

    Caches user data and subscription lists fetched from DBService
    to reduce HTTP calls and improve response time.
    """

    def __init__(self, url: str = REDIS_URL, default_ttl: int = 120):
        self.url = url
        self.default_ttl = default_ttl
        self._redis = None

    async def connect(self) -> None:
        """Initialize Redis connection."""
        try:
            import redis.asyncio as aioredis
            self._redis = aioredis.from_url(
                self.url,
                decode_responses=True,
                socket_connect_timeout=5,
            )
            await self._redis.ping()
            logger.info("Redis cache connected")
        except ImportError:
            logger.warning("redis package not installed, caching disabled")
            self._redis = None
        except Exception as e:
            logger.warning(f"Redis connection failed, caching disabled: {e}")
            self._redis = None

    async def close(self) -> None:
        """Close Redis connection."""
        if self._redis:
            await self._redis.close()

    @property
    def available(self) -> bool:
        return self._redis is not None

    # ── Generic operations ────────────────────────────────────────────────────

    async def get(self, key: str) -> Optional[Any]:
        """Get a cached value by key, returns deserialized JSON or None."""
        if not self._redis:
            return None
        try:
            value = await self._redis.get(f"bot:{key}")
            if value is None:
                return None
            return json.loads(value)
        except Exception as e:
            logger.debug(f"Redis get error for {key}: {e}")
            return None

    async def set(self, key: str, value: Any, ttl: Optional[int] = None) -> None:
        """Cache a value as JSON with optional TTL (seconds)."""
        if not self._redis:
            return
        try:
            serialized = json.dumps(value, default=str)
            await self._redis.set(f"bot:{key}", serialized, ex=ttl or self.default_ttl)
        except Exception as e:
            logger.debug(f"Redis set error for {key}: {e}")

    async def delete(self, key: str) -> None:
        """Delete a cached key."""
        if not self._redis:
            return
        try:
            await self._redis.delete(f"bot:{key}")
        except Exception as e:
            logger.debug(f"Redis delete error for {key}: {e}")

    async def delete_pattern(self, pattern: str) -> None:
        """Delete all keys matching a pattern."""
        if not self._redis:
            return
        try:
            keys = []
            async for key in self._redis.scan_iter(f"bot:{pattern}"):
                keys.append(key)
            if keys:
                await self._redis.delete(*keys)
        except Exception as e:
            logger.debug(f"Redis delete_pattern error for {pattern}: {e}")

    # ── User cache ────────────────────────────────────────────────────────────

    async def get_user(self, telegram_id: int) -> Optional[dict]:
        return await self.get(f"user:{telegram_id}")

    async def set_user(self, telegram_id: int, user_data: dict) -> None:
        await self.set(f"user:{telegram_id}", user_data, ttl=600)

    async def invalidate_user(self, telegram_id: int) -> None:
        await self.delete(f"user:{telegram_id}")

    # ── Subscriptions cache ───────────────────────────────────────────────────

    async def get_subscriptions(self, telegram_id: int) -> Optional[list]:
        return await self.get(f"subs:{telegram_id}")

    async def set_subscriptions(self, telegram_id: int, actions: list) -> None:
        await self.set(f"subs:{telegram_id}", actions, ttl=60)

    async def invalidate_subscriptions(self, telegram_id: int) -> None:
        await self.delete(f"subs:{telegram_id}")
