import datetime as dt
import json
import logging
import os
from pathlib import Path

import pyarrow as pa
import pyarrow.parquet as pq
import redis

logger = logging.getLogger(__name__)


NOTIFICATION_SCHEMA = pa.schema([
    ("telegram_id", pa.int64()),
    ("text", pa.string()),
    ("archived_at", pa.timestamp("s")),
])


def get_redis_client(url: str) -> redis.Redis:
    return redis.from_url(url, decode_responses=True)


def fetch_all_notification_keys(r: redis.Redis) -> list[str]:
    keys = []
    for key in r.scan_iter("bot:notif_history:*"):
        keys.append(key)
    return keys


def extract_telegram_id(key: str) -> int:
    return int(key.split(":")[-1])


def read_notifications(r: redis.Redis, key: str, limit: int = 50) -> list[str]:
    return r.lrange(key, 0, limit - 1)


def build_arrow_table(
    records: list[dict],
) -> pa.Table:
    if not records:
        return pa.table(
            {"telegram_id": [], "text": [], "archived_at": []},
            schema=NOTIFICATION_SCHEMA,
        )

    telegram_ids = [r["telegram_id"] for r in records]
    texts = [r["text"] for r in records]
    timestamps = [r["archived_at"] for r in records]

    return pa.table(
        {
            "telegram_id": pa.array(telegram_ids, type=pa.int64()),
            "text": pa.array(texts, type=pa.string()),
            "archived_at": pa.array(timestamps, type=pa.timestamp("s")),
        },
        schema=NOTIFICATION_SCHEMA,
    )


def write_parquet(table: pa.Table, output_dir: str, suffix: str = "") -> str:
    Path(output_dir).mkdir(parents=True, exist_ok=True)
    ts = dt.datetime.now(dt.timezone.utc).strftime("%Y%m%d_%H%M%S")
    filename = f"notifications_{ts}{suffix}.parquet"
    path = os.path.join(output_dir, filename)
    pq.write_table(table, path, compression="snappy")
    logger.info(f"Wrote {table.num_rows} rows to {path}")
    return path


def read_parquet(path: str) -> pa.Table:
    return pq.read_table(path)


def list_parquet_files(directory: str) -> list[str]:
    d = Path(directory)
    if not d.exists():
        return []
    return sorted(str(f) for f in d.glob("*.parquet"))
