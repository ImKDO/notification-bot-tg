#!/usr/bin/env python3
"""
MLX LoRA fine-tuning pipeline for GitHub Issues Summarization.
Optimized for Apple Silicon (M4, 16 GB RAM).

Usage:
    python main.py data               # download & prepare data
    python main.py train              # LoRA fine-tuning
    python main.py eval               # evaluate on test set
    python main.py infer              # interactive inference
    python main.py infer --text "…"   # one-shot inference
"""

import argparse
import os
from pathlib import Path


def load_env() -> None:
    """Load .env file from the project root (if present)."""
    env_file = Path(__file__).parent / ".env"
    if not env_file.exists():
        return
    with open(env_file) as f:
        for line in f:
            line = line.strip()
            if line and not line.startswith("#") and "=" in line:
                key, value = line.split("=", 1)
                os.environ[key.strip()] = value.strip()


def hf_login() -> None:
    """Login to HuggingFace if HF_TOKEN is set."""
    token = os.getenv("HF_TOKEN")
    if token and token not in ("your_huggingface_token_here", "YOUR_HF_TOKEN_HERE"):
        from huggingface_hub import login
        login(token=token)



def cmd_data(_args: argparse.Namespace) -> None:
    from data import prepare_data
    prepare_data()


def cmd_train(args: argparse.Namespace) -> None:
    from train import train
    train(
        model_name=args.model,
        iters=args.iters,
        batch_size=args.batch_size,
        learning_rate=args.lr,
        lora_layers=args.lora_layers,
        max_seq_length=args.max_seq_length,
        grad_checkpoint=not args.no_grad_checkpoint,
    )


def cmd_eval(args: argparse.Namespace) -> None:
    from evaluate import evaluate
    evaluate(
        model_name=args.model,
        adapter_path=args.adapter_path,
        num_samples=args.num_samples,
    )


def cmd_infer(args: argparse.Namespace) -> None:
    from mlx_lm import generate, load
    from data import format_prompt

    print(f"Loading {args.model} + adapters from {args.adapter_path} …")
    model, tokenizer = load(args.model, adapter_path=args.adapter_path)

    # One-shot mode
    if args.text:
        prompt = format_prompt(args.text)
        response = generate(
            model, tokenizer, prompt=prompt,
            max_tokens=args.max_tokens, verbose=False,
        )
        print(f"\nSummary: {response.strip()}")
        return

    # Interactive mode
    print("\nInteractive mode  (Ctrl-C to exit)")
    print("Enter a GitHub issue body:\n")
    while True:
        try:
            text = input("> ").strip()
            if not text:
                continue
            prompt = format_prompt(text)
            response = generate(
                model, tokenizer, prompt=prompt,
                max_tokens=args.max_tokens, verbose=False,
            )
            print(f"Summary: {response.strip()}\n")
        except (KeyboardInterrupt, EOFError):
            print("\nBye!")
            break


# ── CLI ──────────────────────────────────────────────────────────────────────

def main() -> None:
    parser = argparse.ArgumentParser(
        description="MLX LoRA fine-tuning for GitHub Issues Summarization",
    )
    sub = parser.add_subparsers(dest="command", help="command to run")

    # -- data --
    sub.add_parser("data", help="Download and prepare training data")

    # -- train --
    p_train = sub.add_parser("train", help="Fine-tune model with LoRA")
    p_train.add_argument("--model", default="google/gemma-3-4b-it")
    p_train.add_argument("--iters", type=int, default=200)
    p_train.add_argument("--batch-size", type=int, default=1)
    p_train.add_argument("--lr", type=float, default=1e-5)
    p_train.add_argument("--lora-layers", type=int, default=8)
    p_train.add_argument("--max-seq-length", type=int, default=1024)
    p_train.add_argument("--no-grad-checkpoint", action="store_true",
                         help="Disable gradient checkpointing (uses more memory)")

    # -- eval --
    p_eval = sub.add_parser("eval", help="Evaluate fine-tuned model")
    p_eval.add_argument("--model", default="google/gemma-3-4b-it")
    p_eval.add_argument("--adapter-path", default="results/adapters")
    p_eval.add_argument("--num-samples", type=int, default=20)

    # -- infer --
    p_infer = sub.add_parser("infer", help="Interactive inference")
    p_infer.add_argument("--model", default="google/gemma-3-4b-it")
    p_infer.add_argument("--adapter-path", default="results/adapters")
    p_infer.add_argument("--max-tokens", type=int, default=100)
    p_infer.add_argument("--text", type=str, default=None,
                         help="Issue body to summarize (omit for interactive mode)")

    args = parser.parse_args()
    if not args.command:
        parser.print_help()
        return

    load_env()
    hf_login()

    dispatch = {
        "data": cmd_data,
        "train": cmd_train,
        "eval": cmd_eval,
        "infer": cmd_infer,
    }
    dispatch[args.command](args)


if __name__ == "__main__":
    main()
