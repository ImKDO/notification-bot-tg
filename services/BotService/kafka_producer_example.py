"""
Example Kafka producer for sending notifications to the Notifications topic.
This can be used by other services to send notifications to users via Telegram bot.
"""
import json
import os
from typing import Optional

from kafka import KafkaProducer
from dotenv import load_dotenv

load_dotenv()

KAFKA_BOOTSTRAP_SERVERS = os.getenv("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092")


class NotificationProducer:
    """Kafka producer for sending notifications"""

    def __init__(self, bootstrap_servers: str = KAFKA_BOOTSTRAP_SERVERS):
        self.producer = KafkaProducer(
            bootstrap_servers=bootstrap_servers.split(","),
            value_serializer=lambda v: json.dumps(v).encode("utf-8"),
        )
        self.topic = "Notifications"

    def send_notification(
        self,
        telegram_id: int,
        title: str,
        message: Optional[str] = None,
        service: Optional[str] = None,
        notification_type: Optional[str] = None,
        url: Optional[str] = None,
    ) -> None:
        """
        Send notification to Kafka topic

        Args:
            telegram_id: Telegram user ID
            title: Notification title
            message: Notification message text
            service: Service name (e.g., 'github', 'stackoverflow')
            notification_type: Type of notification (e.g., 'issue', 'pull_request')
            url: URL to the resource
        """
        notification_data = {
            "telegram_id": telegram_id,
            "title": title,
        }

        if message:
            notification_data["message"] = message
        if service:
            notification_data["service"] = service
        if notification_type:
            notification_data["type"] = notification_type
        if url:
            notification_data["url"] = url

        self.producer.send(self.topic, notification_data)
        print(f"Notification sent to Kafka: {notification_data}")

    def flush(self):
        """Flush pending messages"""
        self.producer.flush()

    def close(self):
        """Close the producer"""
        self.producer.close()


# Example usage
if __name__ == "__main__":
    producer = NotificationProducer()

    # Example 1: Simple notification
    producer.send_notification(
        telegram_id=123456789,
        title="Test Notification",
        message="This is a test message",
    )

    # Example 2: GitHub issue notification
    producer.send_notification(
        telegram_id=123456789,
        title="New Issue Created",
        message="Issue #42: Bug in login form",
        service="github",
        notification_type="issue",
        url="https://github.com/user/repo/issues/42",
    )

    # Example 3: GitHub PR notification
    producer.send_notification(
        telegram_id=123456789,
        title="Pull Request Merged",
        message="PR #15: Add new feature - merged by @user",
        service="github",
        notification_type="pull_request",
        url="https://github.com/user/repo/pull/15",
    )

    producer.flush()
    producer.close()
