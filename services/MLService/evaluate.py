"""
Evaluation of the fine-tuned model.

Loads the base model + LoRA adapters via mlx-lm,
generates summaries on the test set, and computes ROUGE metrics.
"""

import json
from pathlib import Path

import numpy as np
from datasets import load_dataset
from mlx_lm import generate, load
from rouge_score import rouge_scorer
from tqdm import tqdm

from config import (
    ADAPTER_DIR,
    DATASET_NAME,
    EVAL_SAMPLES,
    MAX_TOKENS,
    MODEL_NAME,
    OUTPUT_DIR,
)
from data import format_prompt


def evaluate(
    model_name: str = MODEL_NAME,
    adapter_path: str = ADAPTER_DIR,
    num_samples: int = EVAL_SAMPLES,
    max_tokens: int = MAX_TOKENS,
    output_dir: str = OUTPUT_DIR,
) -> dict:
    """Evaluate the fine-tuned model on the test split and return metrics."""

    print(f"\n{'=' * 60}")
    print("  Model Evaluation")
    print(f"{'=' * 60}")
    print(f"  Model:    {model_name}")
    print(f"  Adapter:  {adapter_path}")
    print(f"  Samples:  {num_samples}")
    print(f"{'=' * 60}\n")

    # ── Load model with LoRA adapters ────────────────────────────────────
    print("Loading model + LoRA adapters …")
    model, tokenizer = load(model_name, adapter_path=adapter_path)

    # ── Load test data ───────────────────────────────────────────────────
    print("Loading test dataset …")
    dataset = load_dataset(DATASET_NAME, split="test")

    if num_samples and num_samples < len(dataset):
        indices = np.random.choice(len(dataset), num_samples, replace=False)
        dataset = dataset.select(indices)

    print(f"Evaluating on {len(dataset)} samples …\n")

    # ── Generate predictions ─────────────────────────────────────────────
    predictions: list[str] = []
    references: list[str] = []

    for item in tqdm(dataset, desc="Generating"):
        prompt = format_prompt(item["body"])
        response = generate(
            model,
            tokenizer,
            prompt=prompt,
            max_tokens=max_tokens,
            verbose=False,
        )
        predictions.append(response.strip())
        references.append(item["title"])

    # ── ROUGE metrics ────────────────────────────────────────────────────
    scorer = rouge_scorer.RougeScorer(
        ["rouge1", "rouge2", "rougeL"], use_stemmer=True
    )

    r1, r2, rL = [], [], []
    for pred, ref in zip(predictions, references):
        scores = scorer.score(ref, pred)
        r1.append(scores["rouge1"].fmeasure)
        r2.append(scores["rouge2"].fmeasure)
        rL.append(scores["rougeL"].fmeasure)

    metrics = {
        "rouge1": {"mean": float(np.mean(r1)), "std": float(np.std(r1))},
        "rouge2": {"mean": float(np.mean(r2)), "std": float(np.std(r2))},
        "rougeL": {"mean": float(np.mean(rL)), "std": float(np.std(rL))},
        "num_samples": len(dataset),
        "avg_pred_len": float(np.mean([len(p.split()) for p in predictions])),
        "avg_ref_len": float(np.mean([len(r.split()) for r in references])),
    }

    # ── Print results ────────────────────────────────────────────────────
    print(f"\n{'=' * 60}")
    print("  RESULTS")
    print(f"{'=' * 60}")
    print(f"  ROUGE-1: {metrics['rouge1']['mean']:.4f} (±{metrics['rouge1']['std']:.4f})")
    print(f"  ROUGE-2: {metrics['rouge2']['mean']:.4f} (±{metrics['rouge2']['std']:.4f})")
    print(f"  ROUGE-L: {metrics['rougeL']['mean']:.4f} (±{metrics['rougeL']['std']:.4f})")
    print(f"  Avg prediction length: {metrics['avg_pred_len']:.1f} words")
    print(f"  Avg reference  length: {metrics['avg_ref_len']:.1f} words")
    print(f"{'=' * 60}")

    # ── Save results ─────────────────────────────────────────────────────
    out = Path(output_dir)
    out.mkdir(parents=True, exist_ok=True)

    with open(out / "metrics.json", "w") as f:
        json.dump(metrics, f, indent=2)

    with open(out / "predictions.jsonl", "w") as f:
        for pred, ref, item in zip(predictions, references, dataset):
            f.write(
                json.dumps(
                    {
                        "body": item["body"][:500],
                        "reference": ref,
                        "prediction": pred,
                    }
                )
                + "\n"
            )

    print("\ Sample Predictions:")
    for i in range(min(5, len(predictions))):
        print(f"\n  --- Example {i + 1} ---")
        print(f"  Reference:  {references[i]}")
        print(f"  Prediction: {predictions[i]}")

    print(f"\ Results saved to {out}/")
    return metrics


if __name__ == "__main__":
    evaluate()
