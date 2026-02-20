"""
DAG: cleanup_old_archives

Weekly cleanup of old Parquet files older than 30 days.
"""

import datetime as dt
import os
import sys
from pathlib import Path

from airflow import DAG
from airflow.operators.python import PythonOperator

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from arrow_utils import list_parquet_files
from config import PARQUET_DIR

default_args = {
    "owner": "notification-bot",
    "retries": 1,
    "retry_delay": dt.timedelta(minutes=10),
}

MAX_AGE_DAYS = 30


def _cleanup_old_files(**context):
    files = list_parquet_files(PARQUET_DIR)
    if not files:
        print("No parquet files found")
        return

    cutoff = dt.datetime.now(dt.timezone.utc) - dt.timedelta(days=MAX_AGE_DAYS)
    removed = 0

    for fpath in files:
        mtime = dt.datetime.fromtimestamp(
            os.path.getmtime(fpath), tz=dt.timezone.utc
        )
        if mtime < cutoff:
            os.remove(fpath)
            removed += 1
            print(f"Removed old archive: {fpath}")

    print(f"Cleanup done: removed {removed} / {len(files)} files")


with DAG(
    dag_id="cleanup_old_archives",
    description="Remove Parquet archives older than 30 days",
    default_args=default_args,
    schedule="0 3 * * 0",
    start_date=dt.datetime(2026, 1, 1, tzinfo=dt.timezone.utc),
    catchup=False,
    tags=["notifications", "cleanup", "pyarrow"],
) as dag:

    cleanup_task = PythonOperator(
        task_id="cleanup_old_files",
        python_callable=_cleanup_old_files,
    )
