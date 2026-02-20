"""
DAG: archive_notifications

Periodically reads notification history from Redis for all users,
converts to Arrow columnar format, and saves as a partitioned
Parquet file for analytics / long-term storage.

Schedule: every 6 hours.
"""

import datetime as dt
import sys
from pathlib import Path

from airflow import DAG
from airflow.operators.python import PythonOperator

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from arrow_utils import (
    build_arrow_table,
    extract_telegram_id,
    fetch_all_notification_keys,
    get_redis_client,
    read_notifications,
    write_parquet,
)
from config import NOTIFICATION_HISTORY_LIMIT, PARQUET_DIR, REDIS_URL

default_args = {
    "owner": "notification-bot",
    "retries": 2,
    "retry_delay": dt.timedelta(minutes=5),
}


def _collect_and_archive(**context):
    r = get_redis_client(REDIS_URL)
    keys = fetch_all_notification_keys(r)

    if not keys:
        print("No notification history found in Redis")
        return

    now = dt.datetime.now(dt.timezone.utc)
    records = []
    for key in keys:
        tg_id = extract_telegram_id(key)
        texts = read_notifications(r, key, limit=NOTIFICATION_HISTORY_LIMIT)
        for text in texts:
            records.append({
                "telegram_id": tg_id,
                "text": text,
                "archived_at": now,
            })

    table = build_arrow_table(records)
    path = write_parquet(table, PARQUET_DIR)

    print(f"Archived {table.num_rows} notifications from {len(keys)} users → {path}")
    context["ti"].xcom_push(key="parquet_path", value=path)
    context["ti"].xcom_push(key="row_count", value=table.num_rows)


with DAG(
    dag_id="archive_notifications",
    description="Archive notification history from Redis to Parquet via PyArrow",
    default_args=default_args,
    schedule="0 */6 * * *",
    start_date=dt.datetime(2026, 1, 1, tzinfo=dt.timezone.utc),
    catchup=False,
    tags=["notifications", "pyarrow", "archive"],
) as dag:

    archive_task = PythonOperator(
        task_id="collect_and_archive",
        python_callable=_collect_and_archive,
    )
