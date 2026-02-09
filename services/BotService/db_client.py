import os
from typing import Any, Optional

import httpx
from dotenv import load_dotenv

load_dotenv()

DB_API_URL = os.getenv("DB_API_URL", "http://localhost:8080/api")


class DBClient:
    """HTTP client for database API"""

    def __init__(self, base_url: str = DB_API_URL):
        self.base_url = base_url if base_url.endswith("/") else f"{base_url}/"
        self.client: Optional[httpx.AsyncClient] = None

    async def __aenter__(self):
        self.client = httpx.AsyncClient(base_url=self.base_url, timeout=30.0)
        return self

    async def __aexit__(self, exc_type, exc_val, exc_tb):
        if self.client:
            await self.client.aclose()

    # ── User management ──────────────────────────────────────────────────────

    async def create_user(self, telegram_id: int) -> dict[str, Any]:
        """Create or get existing user by telegram chat id"""
        response = await self.client.post(
            "users",
            json={"idTgChat": telegram_id},
        )
        response.raise_for_status()
        return response.json()

    async def get_user(self, telegram_id: int) -> Optional[dict[str, Any]]:
        """Get user by telegram chat id"""
        response = await self.client.get(f"users/tg-chat/{telegram_id}")
        if response.status_code == 404:
            return None
        response.raise_for_status()
        return response.json()

    async def get_user_by_id(self, user_id: int) -> Optional[dict[str, Any]]:
        """Get user by internal id"""
        response = await self.client.get(f"users/{user_id}")
        if response.status_code == 404:
            return None
        response.raise_for_status()
        return response.json()

    # ── Tokens ───────────────────────────────────────────────────────────────

    async def create_token(
        self,
        telegram_id: int,
        token_value: str,
    ) -> dict[str, Any]:
        """Create authorization token for user"""
        # First get user to link token
        user = await self.get_user(telegram_id)
        if not user:
            user = await self.create_user(telegram_id)
        
        response = await self.client.post(
            "tokens",
            json={
                "value": token_value,
                "user": {"idTgChat": telegram_id},
            },
        )
        response.raise_for_status()
        return response.json()

    async def validate_token(
        self,
        telegram_id: int,
        token_value: str,
        service: str,
    ) -> dict[str, Any]:
        """Send token to DBService for validation (forwarded to CoreService via Kafka)"""
        response = await self.client.post(
            "tokens/validate",
            json={
                "telegramId": telegram_id,
                "token": token_value,
                "service": service,
            },
        )
        response.raise_for_status()
        return response.json()

    async def get_user_tokens(self, user_id: int) -> list[dict[str, Any]]:
        """Get all tokens for user by user id"""
        response = await self.client.get(f"tokens/user/{user_id}")
        response.raise_for_status()
        return response.json()

    # ── Actions (Subscriptions) ──────────────────────────────────────────────

    async def subscribe(
        self,
        telegram_id: int,
        method_name: str,
        query: str,
        service_name: str = "GitHub",
        describe: str = "",
    ) -> dict[str, Any]:
        """Create subscription via the smart /subscribe endpoint.
        
        Auto-resolves service, method, and token by name/telegramId.
        """
        response = await self.client.post(
            "actions/subscribe",
            json={
                "telegramId": telegram_id,
                "methodName": method_name,
                "query": query,
                "serviceName": service_name,
                "describe": describe,
            },
        )
        response.raise_for_status()
        return response.json()

    async def create_action(
        self,
        telegram_id: int,
        service_id: int,
        method_id: int,
        token_id: int,
        query: str = "",
        describe: str = "",
    ) -> dict[str, Any]:
        """Create action (subscription) for user"""
        response = await self.client.post(
            "actions",
            json={
                "user": {"idTgChat": telegram_id},
                "service": {"id": service_id},
                "method": {"id": method_id},
                "token": {"id": token_id},
                "query": query,
                "describe": describe,
            },
        )
        response.raise_for_status()
        return response.json()

    async def get_user_actions(self, user_id: int) -> list[dict[str, Any]]:
        """Get all actions for user by user id"""
        response = await self.client.get(f"actions/user/{user_id}")
        response.raise_for_status()
        return response.json()

    async def get_actions_by_telegram_id(self, telegram_id: int) -> list[dict[str, Any]]:
        """Get all actions for user by telegram chat id"""
        response = await self.client.get(f"actions/telegram/{telegram_id}")
        response.raise_for_status()
        return response.json()

    async def delete_action(self, action_id: int) -> None:
        """Delete action"""
        response = await self.client.delete(f"actions/{action_id}")
        response.raise_for_status()

    # ── Services ─────────────────────────────────────────────────────────────

    async def get_all_services(self) -> list[dict[str, Any]]:
        """Get all available services"""
        response = await self.client.get("services")
        response.raise_for_status()
        return response.json()

    async def get_service(self, service_id: int) -> Optional[dict[str, Any]]:
        """Get service by id"""
        response = await self.client.get(f"services/{service_id}")
        if response.status_code == 404:
            return None
        response.raise_for_status()
        return response.json()

    # ── Tags ─────────────────────────────────────────────────────────────────

    async def create_tag(
        self,
        telegram_id: int,
        tag_name: str,
    ) -> dict[str, Any]:
        """Create tag for user"""
        # First get user or create it
        user = await self.get_user(telegram_id)
        if not user:
            user = await self.create_user(telegram_id)
            
        response = await self.client.post(
            "tags",
            json={
                "user": {"idTgChat": telegram_id},
                "name": tag_name,
            },
        )
        response.raise_for_status()
        return response.json()

    async def get_user_tags(self, user_id: int) -> list[dict[str, Any]]:
        """Get all tags for user by user id"""
        response = await self.client.get(f"tags/user/{user_id}")
        response.raise_for_status()
        return response.json()

    async def delete_tag(self, tag_id: int) -> dict[str, Any]:
        """Delete tag"""
        response = await self.client.delete(f"tags/{tag_id}")
        response.raise_for_status()
        return response.json()

    # ── History Answers ──────────────────────────────────────────────────────

    async def get_user_history(
        self,
        user_id: int,
    ) -> list[dict[str, Any]]:
        """Get history answers for user by user id"""
        response = await self.client.get(f"history-answers/user/{user_id}")
        response.raise_for_status()
        return response.json()

    async def create_history_answer(
        self,
        telegram_id: int,
        content: str,
    ) -> dict[str, Any]:
        """Create history answer for user"""
        response = await self.client.post(
            "history-answers",
            json={
                "user": {"idTgChat": telegram_id},
                "content": content,
            },
        )
        response.raise_for_status()
        return response.json()
