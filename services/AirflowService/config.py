import os

from dotenv import load_dotenv

load_dotenv()

REDIS_URL = os.getenv("REDIS_URL", "redis://localhost:6379")
ML_SERVICE_URL = os.getenv("ML_SERVICE_URL", "http://localhost:8042")
DB_API_URL = os.getenv("DB_API_URL", "http://localhost:8080/api")

PARQUET_DIR = os.getenv("PARQUET_DIR", "/tmp/airflow_parquet")
SUMMARY_MAX_TOKENS = int(os.getenv("SUMMARY_MAX_TOKENS", "200"))
NOTIFICATION_HISTORY_LIMIT = int(os.getenv("NOTIFICATION_HISTORY_LIMIT", "50"))
