import asyncio
import json
import logging
import os
from typing import Callable

from aiokafka import AIOKafkaConsumer
from dotenv import load_dotenv

load_dotenv()

KAFKA_BOOTSTRAP_SERVERS = os.getenv("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092")
KAFKA_TOPIC = "Notifications"
KAFKA_GROUP_ID = os.getenv("KAFKA_GROUP_ID", "bot-service-group")

logger = logging.getLogger(__name__)


class NotificationConsumer:
    """Kafka consumer for Notifications topic"""

    def __init__(
        self,
        bootstrap_servers: str = KAFKA_BOOTSTRAP_SERVERS,
        topic: str = KAFKA_TOPIC,
        group_id: str = KAFKA_GROUP_ID,
    ):
        self.bootstrap_servers = bootstrap_servers
        self.topic = topic
        self.group_id = group_id
        self.consumer = None
        self.running = False

    async def start(self, message_handler: Callable) -> None:
        """Start consuming messages from Kafka"""
        self.consumer = AIOKafkaConsumer(
            self.topic,
            bootstrap_servers=self.bootstrap_servers,
            group_id=self.group_id,
            auto_offset_reset="latest",  # Start from latest messages
            enable_auto_commit=True,
            value_deserializer=lambda m: json.loads(m.decode("utf-8")),
        )

        await self.consumer.start()
        self.running = True
        logger.info(f"Kafka consumer started. Listening to topic: {self.topic}")

        try:
            async for message in self.consumer:
                if not self.running:
                    break
                
                try:
                    logger.info(f"Received message from Kafka: {message.value}")
                    await message_handler(message.value)
                except Exception as e:
                    logger.error(f"Error processing message: {e}", exc_info=True)
        finally:
            await self.stop()

    async def stop(self) -> None:
        """Stop the consumer"""
        self.running = False
        if self.consumer:
            await self.consumer.stop()
            logger.info("Kafka consumer stopped")
