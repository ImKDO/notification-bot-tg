"""
DAG: scheduled_summary

Daily digest: reads each user's recent notifications from the latest
Parquet archive, sends them to MLService for summarization,
and pushes the summary back through the Telegram bot HTTP webhook
(or stores for later retrieval).

Schedule: every day at 09:00 UTC.
"""

import datetime as dt
import sys
from pathlib import Path

import httpx
import pyarrow.parquet as pq

from airflow import DAG
from airflow.operators.python import PythonOperator

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from arrow_utils import list_parquet_files
from config import ML_SERVICE_URL, PARQUET_DIR, REDIS_URL, SUMMARY_MAX_TOKENS

default_args = {
    "owner": "notification-bot",
    "retries": 2,
    "retry_delay": dt.timedelta(minutes=5),
}


def _load_latest_parquet(**context):
    files = list_parquet_files(PARQUET_DIR)
    if not files:
        print("No parquet files found, nothing to summarize")
        return None

    latest = files[-1]
    table = pq.read_table(latest)
    print(f"Loaded {table.num_rows} rows from {latest}")

    grouped: dict[int, list[str]] = {}
    tg_ids = table.column("telegram_id").to_pylist()
    texts = table.column("text").to_pylist()

    for tg_id, text in zip(tg_ids, texts):
        grouped.setdefault(tg_id, []).append(text)

    context["ti"].xcom_push(key="grouped_notifications", value=grouped)
    context["ti"].xcom_push(key="parquet_file", value=latest)


def _generate_summaries(**context):
    grouped = context["ti"].xcom_pull(
        task_ids="load_latest_parquet",
        key="grouped_notifications",
    )
    if not grouped:
        print("No notifications to summarize")
        return

    summaries = {}
    for tg_id_str, notifications in grouped.items():
        tg_id = int(tg_id_str)
        if not notifications:
            continue

        try:
            resp = httpx.post(
                f"{ML_SERVICE_URL}/summarize",
                json={
                    "notifications": notifications[:30],
                    "max_tokens": SUMMARY_MAX_TOKENS,
                },
                timeout=120.0,
            )
            resp.raise_for_status()
            summary = resp.json().get("summary", "")
            summaries[tg_id] = summary
            print(f"User {tg_id}: generated summary ({len(summary)} chars)")
        except Exception as e:
            print(f"User {tg_id}: ML summarization failed: {e}")

    context["ti"].xcom_push(key="summaries", value=summaries)


def _store_summaries(**context):
    import redis

    summaries = context["ti"].xcom_pull(
        task_ids="generate_summaries",
        key="summaries",
    )
    if not summaries:
        print("No summaries to store")
        return

    r = redis.from_url(REDIS_URL, decode_responses=True)

    for tg_id, summary in summaries.items():
        key = f"bot:daily_summary:{tg_id}"
        r.set(key, summary, ex=60 * 60 * 24)
        print(f"Stored daily summary for user {tg_id}")

    print(f"Stored {len(summaries)} daily summaries in Redis")


with DAG(
    dag_id="scheduled_summary",
    description="Daily ML-powered notification digest from Parquet archives",
    default_args=default_args,
    schedule="0 9 * * *",
    start_date=dt.datetime(2026, 1, 1, tzinfo=dt.timezone.utc),
    catchup=False,
    tags=["notifications", "ml", "summary", "pyarrow"],
) as dag:

    load_task = PythonOperator(
        task_id="load_latest_parquet",
        python_callable=_load_latest_parquet,
    )

    summarize_task = PythonOperator(
        task_id="generate_summaries",
        python_callable=_generate_summaries,
    )

    store_task = PythonOperator(
        task_id="store_summaries",
        python_callable=_store_summaries,
    )

    load_task >> summarize_task >> store_task
