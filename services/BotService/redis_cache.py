import json
import logging
import os
from typing import Any, Optional

from dotenv import load_dotenv

load_dotenv()

REDIS_URL = os.getenv("REDIS_URL", "redis://localhost:6379")

logger = logging.getLogger(__name__)


class RedisCache:

    def __init__(self, url: str = REDIS_URL, default_ttl: int = 120):
        self.url = url
        self.default_ttl = default_ttl
        self._redis = None

    async def connect(self) -> None:
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
        if self._redis:
            await self._redis.close()

    @property
    def available(self) -> bool:
        return self._redis is not None

    async def get(self, key: str) -> Optional[Any]:
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
        if not self._redis:
            return
        try:
            serialized = json.dumps(value, default=str)
            await self._redis.set(f"bot:{key}", serialized, ex=ttl or self.default_ttl)
        except Exception as e:
            logger.debug(f"Redis set error for {key}: {e}")

    async def delete(self, key: str) -> None:
        if not self._redis:
            return
        try:
            await self._redis.delete(f"bot:{key}")
        except Exception as e:
            logger.debug(f"Redis delete error for {key}: {e}")

    async def delete_pattern(self, pattern: str) -> None:
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

    async def get_user(self, telegram_id: int) -> Optional[dict]:
        return await self.get(f"user:{telegram_id}")

    async def set_user(self, telegram_id: int, user_data: dict) -> None:
        await self.set(f"user:{telegram_id}", user_data, ttl=600)

    async def invalidate_user(self, telegram_id: int) -> None:
        await self.delete(f"user:{telegram_id}")

    async def get_subscriptions(self, telegram_id: int) -> Optional[list]:
        return await self.get(f"subs:{telegram_id}")

    async def set_subscriptions(self, telegram_id: int, actions: list) -> None:
        await self.set(f"subs:{telegram_id}", actions, ttl=60)

    async def invalidate_subscriptions(self, telegram_id: int) -> None:
        await self.delete(f"subs:{telegram_id}")

    async def push_notification(
        self,
        telegram_id: int,
        text: str,
        max_stored: int = 50,
    ) -> None:
        if not self._redis:
            return
        key = f"bot:notif_history:{telegram_id}"
        try:
            await self._redis.lpush(key, text)
            await self._redis.ltrim(key, 0, max_stored - 1)
            await self._redis.expire(key, 60 * 60 * 24 * 7)
        except Exception as e:
            logger.debug(f"Redis push_notification error: {e}")

    async def get_notification_history(
        self,
        telegram_id: int,
        limit: int = 20,
    ) -> list[str]:
        if not self._redis:
            return []
        key = f"bot:notif_history:{telegram_id}"
        try:
            items = await self._redis.lrange(key, 0, limit - 1)
            return items if items else []
        except Exception as e:
            logger.debug(f"Redis get_notification_history error: {e}")
            return []

    async def clear_notification_history(self, telegram_id: int) -> None:
        if not self._redis:
            return
        try:
            await self._redis.delete(f"bot:notif_history:{telegram_id}")
        except Exception as e:
            logger.debug(f"Redis clear_notification_history error: {e}")
