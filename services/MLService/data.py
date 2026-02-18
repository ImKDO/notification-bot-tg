"""
Data loading and preparation for MLX LoRA training.
Downloads the HuggingFace dataset and writes JSONL files
that mlx-lm expects (train.jsonl, valid.jsonl with {"text": ...} per line).
"""

import json
from pathlib import Path

from datasets import load_dataset

from config import DATASET_NAME, DATA_DIR



def format_example(body: str, title: str) -> str:
    """Training example — prompt + answer in Gemma chat format."""
    return (
        "<start_of_turn>user\n"
        "Summarize the following GitHub issue in one concise sentence:\n\n"
        f"{body}<end_of_turn>\n"
        "<start_of_turn>model\n"
        f"{title}<end_of_turn>"
    )


def format_prompt(body: str) -> str:
    """Inference prompt — no answer, model continues from here."""
    return (
        "<start_of_turn>user\n"
        "Summarize the following GitHub issue in one concise sentence:\n\n"
        f"{body}<end_of_turn>\n"
        "<start_of_turn>model\n"
    )



def prepare_data(data_dir: str = DATA_DIR) -> Path:
    """
    Download dataset and save as JSONL files for mlx-lm.

    If the files already exist they are reused (delete them to re-download).
    Returns the Path to the data directory.
    """
    data_path = Path(data_dir)
    data_path.mkdir(parents=True, exist_ok=True)

    train_file = data_path / "train.jsonl"
    valid_file = data_path / "valid.jsonl"

    if train_file.exists() and valid_file.exists():
        train_count = sum(1 for _ in open(train_file))
        valid_count = sum(1 for _ in open(valid_file))
        print(f"Data already exists: {train_count} train, {valid_count} valid")
        return data_path

    print(f"Downloading dataset: {DATASET_NAME} …")
    dataset = load_dataset(DATASET_NAME)
    print(f"   train={len(dataset['train'])}  test={len(dataset['test'])}")

    with open(train_file, "w") as f:
        for item in dataset["train"]:
            line = json.dumps({"text": format_example(item["body"], item["title"])})
            f.write(line + "\n")

    with open(valid_file, "w") as f:
        for item in dataset["test"]:
            line = json.dumps({"text": format_example(item["body"], item["title"])})
            f.write(line + "\n")

    print(f" Saved {len(dataset['train'])} train → {train_file}")
    print(f" Saved {len(dataset['test'])} valid → {valid_file}")
    return data_path
