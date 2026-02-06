"""
MLX LoRA fine-tuning for Gemma.

Calls ``python -m mlx_lm lora`` to run LoRA training —
the most reliable way to use mlx-lm on Apple Silicon.
"""

import subprocess
import sys
from pathlib import Path

from config import (
    ADAPTER_DIR,
    BATCH_SIZE,
    DATA_DIR,
    GRAD_CHECKPOINT,
    LEARNING_RATE,
    LORA_LAYERS,
    MAX_SEQ_LENGTH,
    MODEL_NAME,
    SAVE_EVERY,
    STEPS_PER_EVAL,
    STEPS_PER_REPORT,
    TRAIN_ITERS,
)
from data import prepare_data


def train(
    model_name: str = MODEL_NAME,
    iters: int = TRAIN_ITERS,
    batch_size: int = BATCH_SIZE,
    learning_rate: float = LEARNING_RATE,
    lora_layers: int = LORA_LAYERS,
    max_seq_length: int = MAX_SEQ_LENGTH,
    grad_checkpoint: bool = GRAD_CHECKPOINT,
    adapter_dir: str = ADAPTER_DIR,
    data_dir: str = DATA_DIR,
) -> None:
    """Run LoRA fine-tuning via ``python -m mlx_lm lora``."""

    # 1. Prepare data (downloads once, then reuses)
    data_path = prepare_data(data_dir)

    adapter_path = Path(adapter_dir)
    adapter_path.mkdir(parents=True, exist_ok=True)

    # 2. Print summary
    print(f"\n{'=' * 60}")
    print("  MLX LoRA Training")
    print(f"{'=' * 60}")
    print(f"  Model:           {model_name}")
    print(f"  Iterations:      {iters}")
    print(f"  Batch size:      {batch_size}")
    print(f"  Learning rate:   {learning_rate}")
    print(f"  LoRA layers:     {lora_layers}")
    print(f"  Max seq length:  {max_seq_length}")
    print(f"  Grad checkpoint: {grad_checkpoint}")
    print(f"  Adapter dir:     {adapter_path}")
    print(f"  Data dir:        {data_path}")
    print(f"{'=' * 60}\n")

    # 3. Build CLI command
    cmd = [
        sys.executable, "-m", "mlx_lm", "lora",
        "--model", model_name,
        "--train",
        "--data", str(data_path),
        "--iters", str(iters),
        "--batch-size", str(batch_size),
        "--num-layers", str(lora_layers),
        "--adapter-path", str(adapter_path),
        "--learning-rate", str(learning_rate),
        "--steps-per-eval", str(STEPS_PER_EVAL),
        "--steps-per-report", str(STEPS_PER_REPORT),
        "--save-every", str(SAVE_EVERY),
        "--max-seq-length", str(max_seq_length),
    ]
    if grad_checkpoint:
        cmd.append("--grad-checkpoint")

    print(f"$ {' '.join(cmd)}\n")
    subprocess.check_call(cmd)

    print(f"\n✅ Training complete! Adapters → {adapter_path}")


if __name__ == "__main__":
    train()
