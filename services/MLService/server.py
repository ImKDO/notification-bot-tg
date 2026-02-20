"""
FastAPI server for MLService.

Exposes a /summarize endpoint that takes a list of notification texts
and returns a concise summary using the fine-tuned LoRA model.
"""

import logging
import os
from contextlib import asynccontextmanager
from pathlib import Path

from fastapi import FastAPI, HTTPException
from pydantic import BaseModel

from config import ADAPTER_DIR, MAX_TOKENS, MODEL_NAME

logger = logging.getLogger(__name__)

# ── Global model state ────────────────────────────────────────────────────────

_model = None
_tokenizer = None


def _load_model():
    """Load the base model + LoRA adapters once at startup."""
    global _model, _tokenizer
    from mlx_lm import load

    model_name = os.getenv("ML_MODEL_NAME", MODEL_NAME)
    adapter_path = os.getenv("ML_ADAPTER_PATH", ADAPTER_DIR)

    if not Path(adapter_path).exists():
        logger.warning(
            f"Adapter path {adapter_path} not found, loading base model only"
        )
        adapter_path = None

    logger.info(f"Loading model {model_name} (adapters: {adapter_path}) …")
    _model, _tokenizer = load(model_name, adapter_path=adapter_path)
    logger.info("Model loaded successfully")


@asynccontextmanager
async def lifespan(app: FastAPI):
    """Load the model on startup, release on shutdown."""
    _load_model()
    yield
    logger.info("MLService shutting down")


app = FastAPI(
    title="MLService – Notification Summarizer",
    version="1.0.0",
    lifespan=lifespan,
)


# ── Request / Response schemas ────────────────────────────────────────────────


class SummarizeRequest(BaseModel):
    """List of notification texts to summarize."""

    notifications: list[str]
    max_tokens: int = MAX_TOKENS


class SummarizeResponse(BaseModel):
    summary: str


# ── Endpoints ─────────────────────────────────────────────────────────────────


def _build_prompt(notifications: list[str]) -> str:
    """Build a prompt that asks the model to summarize recent notifications."""
    joined = "\n---\n".join(notifications)
    return (
        "<start_of_turn>user\n"
        "Summarize the following list of notifications "
        "into a brief overview in Russian. "
        "Group by topic if possible. Be concise.\n\n"
        f"{joined}<end_of_turn>\n"
        "<start_of_turn>model\n"
    )


@app.post("/summarize", response_model=SummarizeResponse)
async def summarize(request: SummarizeRequest):
    """Summarize a batch of notifications into a short digest."""
    if not request.notifications:
        raise HTTPException(status_code=400, detail="notifications list is empty")

    if _model is None or _tokenizer is None:
        raise HTTPException(status_code=503, detail="Model not loaded yet")

    from mlx_lm import generate

    prompt = _build_prompt(request.notifications)

    try:
        result = generate(
            _model,
            _tokenizer,
            prompt=prompt,
            max_tokens=request.max_tokens,
            verbose=False,
        )
        return SummarizeResponse(summary=result.strip())
    except Exception as e:
        logger.error(f"Generation failed: {e}", exc_info=True)
        raise HTTPException(status_code=500, detail="Summarization failed")


@app.get("/health")
async def health():
    return {
        "status": "ok",
        "model_loaded": _model is not None,
    }
