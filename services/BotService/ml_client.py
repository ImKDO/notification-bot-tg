"""HTTP client for MLService summarization API."""

import logging
import os
from typing import Optional

import httpx
from dotenv import load_dotenv

load_dotenv()

ML_SERVICE_URL = os.getenv("ML_SERVICE_URL", "http://localhost:8042")

logger = logging.getLogger(__name__)


class MLClient:
    """Async HTTP client for MLService."""

    def __init__(self, base_url: str = ML_SERVICE_URL, timeout: float = 120.0):
        self.base_url = base_url
        self.timeout = timeout

    async def summarize(
        self,
        notifications: list[str],
        max_tokens: int = 100,
    ) -> Optional[str]:
        """Send notifications to MLService and get a summary back.

        Returns the summary string, or None on failure.
        """
        if not notifications:
            return None

        try:
            async with httpx.AsyncClient(timeout=self.timeout) as client:
                response = await client.post(
                    f"{self.base_url}/summarize",
                    json={
                        "notifications": notifications,
                        "max_tokens": max_tokens,
                    },
                )
                response.raise_for_status()
                data = response.json()
                return data.get("summary")
        except httpx.ConnectError:
            logger.warning("MLService is not available")
            return None
        except httpx.HTTPStatusError as e:
            logger.error(f"MLService returned error: {e.response.status_code}")
            return None
        except Exception as e:
            logger.error(f"MLService request failed: {e}", exc_info=True)
            return None

    async def health(self) -> bool:
        """Check if MLService is healthy."""
        try:
            async with httpx.AsyncClient(timeout=5.0) as client:
                response = await client.get(f"{self.base_url}/health")
                return response.status_code == 200
        except Exception:
            return False
