"""
Configuration for MLX LoRA fine-tuning.
Optimized for Apple M4 with 16 GB RAM.
"""

MODEL_NAME = "google/gemma-3-4b-it"

DATASET_NAME = "Aliyah-20146588/github-issues"

OUTPUT_DIR = "results"
DATA_DIR = "results/data"
ADAPTER_DIR = "results/adapters"

TRAIN_ITERS = 200
BATCH_SIZE = 1
LEARNING_RATE = 1e-5
LORA_LAYERS = 8       
LORA_RANK = 8         
MAX_SEQ_LENGTH = 1024 
GRAD_CHECKPOINT = True
SAVE_EVERY = 100
STEPS_PER_EVAL = 50
STEPS_PER_REPORT = 10

EVAL_SAMPLES = 20
MAX_TOKENS = 100
TEMPERATURE = 0.7
TOP_P = 0.9
