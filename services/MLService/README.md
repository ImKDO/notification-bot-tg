## Структура

```
MLService/
├── main.py        # CLI: data / train / eval / infer
├── train.py       # LoRA обучение через mlx-lm
├── evaluate.py    # Оценка (ROUGE) через mlx-lm
├── data.py        # Загрузка и подготовка данных
├── config.py      # Все параметры в одном месте
├── pyproject.toml # Зависимости (только MLX, без PyTorch)
├── run.sh         # Быстрый запуск полного пайплайна
└── results/
    ├── adapters/  # LoRA-адаптеры после обучения
    └── data/      # train.jsonl, valid.jsonl
```

## Быстрый старт

```bash
# 1. Установить зависимости
uv sync

# 2. Настроить токен HuggingFace
echo "HF_TOKEN=hf_ваш_токен" > .env

# 3. Запустить полный пайплайн
./run.sh
```

Или:

```bash
# Подготовить данные
uv run python main.py data

# Обучить (LoRA)
uv run python main.py train

# Оценить
uv run python main.py eval

# Интерактивный режим
uv run python main.py infer
```

## Параметры обучения

| Параметр | По умолчанию | Описание |
|----------|-------------|----------|
| `--model` | `google/gemma-3-4b-it` | Модель HuggingFace |
| `--iters` | `200` | Кол-во итераций |
| `--batch-size` | `1` | Размер батча (1 для 16 GB) |
| `--lr` | `1e-5` | Learning rate |
| `--lora-layers` | `8` | Кол-во слоёв с LoRA |
| `--max-seq-length` | `1024` | Макс. длина последовательности |
| `--no-grad-checkpoint` | off | Отключить gradient checkpointing |

Пример с кастомными параметрами:

```bash
uv run python main.py train \
    --iters 500 \
    --batch-size 1 \
    --lr 2e-5 \
    --lora-layers 16 \
    --max-seq-length 512
```

> **Совет:** если памяти не хватает — уменьшите `--max-seq-length` до 512
> и `--lora-layers` до 4.

## Оценка

```bash
uv run python main.py eval --num-samples 50
```

Результаты сохраняются в `results/metrics.json` и `results/predictions.jsonl`.

## Inference

Одиночный запрос:

```bash
uv run python main.py infer --text "Bug: app crashes when uploading files > 10MB"
```

Интерактивный режим:

```bash
uv run python main.py infer
> Bug: app crashes when uploading files > 10MB
Summary: Fix file upload crash for large files
```

## Датасет

[Aliyah-20146588/github-issues](https://huggingface.co/datasets/Aliyah-20146588/github-issues)