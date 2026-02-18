#!/usr/bin/env bash
set -euo pipefail

echo "🍏 MLX LoRA Fine-tuning Pipeline"
echo "================================="

# .env check
if [ -f .env ]; then
    echo "✅ .env found"
else
    echo "⚠️  No .env — create one with HF_TOKEN=… if the model requires authentication"
fi

echo ""
echo "Step 1/3: Prepare data"
uv run python main.py data

echo ""
echo "Step 2/3: LoRA training"
uv run python main.py train --iters 200 --batch-size 1 --lora-layers 8

echo ""
echo "Step 3/3: Evaluate"
uv run python main.py eval --num-samples 20

echo ""
echo "✅ Done!"
echo "   Run 'uv run python main.py infer' for interactive mode."
