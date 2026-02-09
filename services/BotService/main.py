import asyncio
import logging
import os
import re

import httpx
from aiogram import Bot, Dispatcher, F, Router
from aiogram.filters import CommandStart
from aiogram.fsm.context import FSMContext
from aiogram.fsm.state import State, StatesGroup
from aiogram.types import (
    CallbackQuery,
    InlineKeyboardButton,
    InlineKeyboardMarkup,
    Message,
)
from dotenv import load_dotenv

from db_client import DBClient
from kafka_consumer import NotificationConsumer

load_dotenv()

BOT_TOKEN = os.getenv("BOT_TOKEN", "")

bot = Bot(token=BOT_TOKEN)
dp = Dispatcher()
router = Router()
db = DBClient()
kafka_consumer = NotificationConsumer()


# ── FSM States ───────────────────────────────────────────────────────────────

class AuthStates(StatesGroup):
    waiting_for_token = State()
    waiting_for_resource_link = State()


# ── Keyboards ────────────────────────────────────────────────────────────────

def main_menu_kb() -> InlineKeyboardMarkup:
    return InlineKeyboardMarkup(inline_keyboard=[
        [InlineKeyboardButton(text="1. Авторизовать сервис", callback_data="menu:auth")],
        [InlineKeyboardButton(text="2. Подписки", callback_data="menu:subscribe")],
        [InlineKeyboardButton(text="3. Настроить теги", callback_data="menu:tags")],
        [InlineKeyboardButton(text="4. История уведомлений", callback_data="menu:history")],
    ])


def auth_service_kb() -> InlineKeyboardMarkup:
    return InlineKeyboardMarkup(inline_keyboard=[
        [InlineKeyboardButton(text="1. Github", callback_data="auth:github")],
        [InlineKeyboardButton(text="2. Stackoverflow", callback_data="auth:stackoverflow")],
        [InlineKeyboardButton(text="⬅️ Назад", callback_data="back:main")],
    ])


def subscribe_kb() -> InlineKeyboardMarkup:
    return InlineKeyboardMarkup(inline_keyboard=[
        [InlineKeyboardButton(text="➕ Новая подписка", callback_data="menu:new_sub")],
        [InlineKeyboardButton(text="📋 Мои подписки", callback_data="menu:my_subs")],
        [InlineKeyboardButton(text="⬅️ Назад", callback_data="back:main")],
    ])


def new_subscribe_kb() -> InlineKeyboardMarkup:
    return InlineKeyboardMarkup(inline_keyboard=[
        [InlineKeyboardButton(text="Issue", callback_data="sub:issue")],
        [InlineKeyboardButton(text="Pull Request", callback_data="sub:pull_request")],
        [InlineKeyboardButton(text="Commit", callback_data="sub:commit")],
        [InlineKeyboardButton(text="Github Actions", callback_data="sub:actions")],
        [InlineKeyboardButton(text="Branch", callback_data="sub:branch")],
        [InlineKeyboardButton(text="⬅️ Назад", callback_data="menu:subscribe")],
    ])


def notification_period_kb() -> InlineKeyboardMarkup:
    return InlineKeyboardMarkup(inline_keyboard=[
        [InlineKeyboardButton(text="⬅️ Назад", callback_data="back:main")],
    ])


# ── Handlers ─────────────────────────────────────────────────────────────────

@router.message(CommandStart())
async def cmd_start(message: Message, state: FSMContext) -> None:
    await state.clear()
    # Register user in DB
    try:
        async with db:
            await db.create_user(
                telegram_id=message.from_user.id,
            )
    except httpx.ConnectError:
        await message.answer("⚠️ Ошибка: База данных недоступна. Пожалуйста, убедитесь, что DBService запущен.")
        return
    await message.answer("Главное меню:", reply_markup=main_menu_kb())


# ── Back to main menu ────────────────────────────────────────────────────────

@router.callback_query(F.data == "back:main")
async def back_to_main(callback: CallbackQuery, state: FSMContext) -> None:
    await state.clear()
    await callback.message.edit_text("Главное меню:", reply_markup=main_menu_kb())
    await callback.answer()


# ── 1. Авторизовать сервис ───────────────────────────────────────────────────

@router.callback_query(F.data == "menu:auth")
async def menu_auth(callback: CallbackQuery) -> None:
    await callback.message.edit_text(
        "Выберите сервис для авторизации:",
        reply_markup=auth_service_kb(),
    )
    await callback.answer()


@router.callback_query(F.data.startswith("auth:"))
async def auth_select_service(callback: CallbackQuery, state: FSMContext) -> None:
    service = callback.data.split(":")[1]
    await state.update_data(service=service)
    await callback.message.edit_text(
        f"Вы выбрали: {service.capitalize()}\n\nВведите токен:"
    )
    await state.set_state(AuthStates.waiting_for_token)
    await callback.answer()


@router.message(AuthStates.waiting_for_token)
async def process_token(message: Message, state: FSMContext) -> None:
    token = message.text.strip()
    data = await state.get_data()
    service = data.get("service", "")

    # Send token to DBService for validation via HTTP
    try:
        async with db:
            await db.validate_token(
                telegram_id=message.from_user.id,
                token_value=token,
                service=service,
            )
        await message.answer(
            "⏳ Токен отправлен на валидацию...\n"
            "Результат придёт в уведомлении.",
            reply_markup=main_menu_kb(),
        )
    except httpx.ConnectError:
        await message.answer(
            "⚠️ Ошибка: DBService недоступен.",
            reply_markup=main_menu_kb(),
        )
    except Exception as e:
        logging.error(f"Failed to send token for validation: {e}")
        await message.answer(
            "⚠️ Ошибка при отправке токена. Попробуйте позже.",
            reply_markup=main_menu_kb(),
        )

    await state.clear()


# Mapping from callback sub types to DB method names
SUB_TYPE_TO_METHOD = {
    "issue": "ISSUE",
    "pull_request": "PULL_REQUEST",
    "commit": "COMMIT",
    "actions": "GITHUB_ACTIONS",
    "branch": "BRANCH",
}

SUB_TYPE_LABELS = {
    "issue": "Issue",
    "pull_request": "Pull Request",
    "commit": "Commit",
    "actions": "Github Actions",
    "branch": "Branch",
}

# URL hints per subscription type
SUB_TYPE_HINTS = {
    "issue": (
        "Пришлите ссылку на Issue:\n"
        "<code>https://github.com/owner/repo/issues/123</code>"
    ),
    "pull_request": (
        "Пришлите ссылку на Pull Request:\n"
        "<code>https://github.com/owner/repo/pull/123</code>"
    ),
    "commit": (
        "Пришлите ссылку на Commit:\n"
        "<code>https://github.com/owner/repo/commit/abc1234</code>"
    ),
    "actions": (
        "Пришлите ссылку на GitHub Actions:\n"
        "<code>https://github.com/owner/repo/actions</code>\n"
        "или на конкретный workflow:\n"
        "<code>https://github.com/owner/repo/actions/workflows/ci.yml</code>"
    ),
    "branch": (
        "Пришлите ссылку на ветку:\n"
        "<code>https://github.com/owner/repo/tree/branch-name</code>\n"
        "или\n"
        "<code>https://github.com/owner/repo/commits/branch-name</code>"
    ),
}

# URL validation patterns (mirror GithubService parsers)
URL_PATTERNS = {
    "issue": re.compile(r"github\.com/[^/]+/[^/]+/issues/\d+"),
    "pull_request": re.compile(r"github\.com/[^/]+/[^/]+/pull/\d+"),
    "commit": re.compile(r"github\.com/[^/]+/[^/]+/commit/[0-9a-fA-F]+"),
    "actions": re.compile(r"github\.com/[^/]+/[^/]+/actions"),
    "branch": re.compile(r"github\.com/[^/]+/[^/]+/(?:tree|commits)/.+"),
}


@router.callback_query(F.data == "menu:subscribe")
async def menu_subscribe(callback: CallbackQuery) -> None:
    await callback.message.edit_text(
        "📌 Подписки:",
        reply_markup=subscribe_kb(),
    )
    await callback.answer()


@router.callback_query(F.data == "menu:new_sub")
async def menu_new_subscribe(callback: CallbackQuery) -> None:
    await callback.message.edit_text(
        "Выберите тип подписки:",
        reply_markup=new_subscribe_kb(),
    )
    await callback.answer()


@router.callback_query(F.data.startswith("sub:"))
async def subscribe_select(callback: CallbackQuery, state: FSMContext) -> None:
    sub_type = callback.data.split(":")[1]
    await state.update_data(sub_type=sub_type)
    label = SUB_TYPE_LABELS.get(sub_type, sub_type)
    hint = SUB_TYPE_HINTS.get(sub_type, "Пришлите ссылку на GitHub ресурс:")
    await callback.message.edit_text(
        f"Вы выбрали: {label}\n\n{hint}",
        parse_mode="HTML",
    )
    await state.set_state(AuthStates.waiting_for_resource_link)
    await callback.answer()


@router.message(AuthStates.waiting_for_resource_link)
async def process_resource_link(message: Message, state: FSMContext) -> None:
    link = message.text.strip()
    data = await state.get_data()
    sub_type = data.get("sub_type", "")
    method_name = SUB_TYPE_TO_METHOD.get(sub_type, "")
    label = SUB_TYPE_LABELS.get(sub_type, sub_type)

    if not method_name:
        await message.answer(
            "⚠️ Неизвестный тип подписки. Попробуйте снова.",
            reply_markup=main_menu_kb(),
        )
        await state.clear()
        return

    # Validate URL format
    pattern = URL_PATTERNS.get(sub_type)
    if pattern and not pattern.search(link):
        hint = SUB_TYPE_HINTS.get(sub_type, "")
        await message.answer(
            f"⚠️ Неверный формат ссылки для {label}.\n\n{hint}",
            parse_mode="HTML",
            reply_markup=main_menu_kb(),
        )
        await state.clear()
        return

    try:
        async with db:
            await db.subscribe(
                telegram_id=message.from_user.id,
                method_name=method_name,
                query=link,
                service_name="GitHub",
                describe=label,
            )
        await message.answer(
            f"⏳ Подписка отправлена на обработку...\n\n"
            f"Тип: {label}\n"
            f"Ресурс: {link}\n\n"
            "Результат придёт в уведомлении.",
            reply_markup=main_menu_kb(),
        )
    except httpx.HTTPStatusError as e:
        error_detail = ""
        try:
            error_body = e.response.json()
            error_detail = error_body.get("error", str(e))
        except Exception:
            error_detail = str(e)
        await message.answer(
            f"⚠️ Ошибка: {error_detail}",
            reply_markup=main_menu_kb(),
        )
    except httpx.ConnectError:
        await message.answer(
            "⚠️ Ошибка: DBService недоступен.",
            reply_markup=main_menu_kb(),
        )
    except Exception as e:
        logging.error(f"Failed to create subscription: {e}")
        await message.answer(
            "⚠️ Ошибка при создании подписки. Попробуйте позже.",
            reply_markup=main_menu_kb(),
        )

    await state.clear()


# ── Мои подписки / Отписка ───────────────────────────────────────────────────

METHOD_ICONS = {
    "ISSUE": "🐛",
    "PULL_REQUEST": "🔀",
    "COMMIT": "📝",
    "BRANCH": "🌿",
    "GITHUB_ACTIONS": "⚙️",
}

METHOD_LABELS = {
    "ISSUE": "Issue",
    "PULL_REQUEST": "Pull Request",
    "COMMIT": "Commit",
    "BRANCH": "Branch",
    "GITHUB_ACTIONS": "GitHub Actions",
}


def _format_subscription_list(actions: list[dict]) -> tuple[str, InlineKeyboardMarkup]:
    """Format subscriptions into text + keyboard with unsubscribe buttons."""
    if not actions:
        text = "📋 <b>Мои подписки</b>\n\nУ вас пока нет активных подписок."
        kb = InlineKeyboardMarkup(inline_keyboard=[
            [InlineKeyboardButton(text="➕ Новая подписка", callback_data="menu:new_sub")],
            [InlineKeyboardButton(text="⬅️ Назад", callback_data="menu:subscribe")],
        ])
        return text, kb

    text = "📋 <b>Мои подписки</b>\n\n"
    buttons = []
    for i, action in enumerate(actions, 1):
        method_name = action.get("method", {}).get("name", "?") if action.get("method") else "?"
        query = action.get("query", "")
        action_id = action.get("id", 0)
        icon = METHOD_ICONS.get(method_name, "🔔")
        label = METHOD_LABELS.get(method_name, method_name)

        # Shorten URL for display
        short_url = query.replace("https://github.com/", "") if query else "—"

        text += f"{i}. {icon} <b>{label}</b>\n    {short_url}\n\n"
        buttons.append([
            InlineKeyboardButton(
                text=f"❌ {i}. {label} — {short_url[:30]}",
                callback_data=f"unsub:{action_id}",
            )
        ])

    buttons.append([InlineKeyboardButton(text="⬅️ Назад", callback_data="menu:subscribe")])
    kb = InlineKeyboardMarkup(inline_keyboard=buttons)
    return text, kb


@router.callback_query(F.data == "menu:my_subs")
async def menu_my_subs(callback: CallbackQuery) -> None:
    try:
        async with db:
            actions = await db.get_actions_by_telegram_id(callback.from_user.id)
        text, kb = _format_subscription_list(actions)
        await callback.message.edit_text(text, reply_markup=kb, parse_mode="HTML")
    except httpx.ConnectError:
        await callback.message.edit_text(
            "⚠️ Ошибка: DBService недоступен.",
            reply_markup=subscribe_kb(),
        )
    except Exception as e:
        logging.error(f"Failed to fetch subscriptions: {e}")
        await callback.message.edit_text(
            "⚠️ Ошибка при загрузке подписок.",
            reply_markup=subscribe_kb(),
        )
    await callback.answer()


@router.callback_query(F.data.startswith("unsub:"))
async def unsubscribe_action(callback: CallbackQuery) -> None:
    action_id = int(callback.data.split(":")[1])
    try:
        async with db:
            await db.delete_action(action_id)
            # Refresh the list
            actions = await db.get_actions_by_telegram_id(callback.from_user.id)
        text, kb = _format_subscription_list(actions)
        text = "✅ Подписка удалена!\n\n" + text
        await callback.message.edit_text(text, reply_markup=kb, parse_mode="HTML")
    except httpx.ConnectError:
        await callback.message.edit_text(
            "⚠️ Ошибка: DBService недоступен.",
            reply_markup=subscribe_kb(),
        )
    except Exception as e:
        logging.error(f"Failed to unsubscribe: {e}")
        await callback.message.edit_text(
            "⚠️ Ошибка при отписке. Попробуйте позже.",
            reply_markup=subscribe_kb(),
        )
    await callback.answer()


# ── 2. Настроить теги ───────────────────────────────────────────────────────

@router.callback_query(F.data == "menu:tags")
async def menu_tags(callback: CallbackQuery) -> None:
    await callback.message.edit_text(
        "🏷 Настройка тегов (в разработке)",
        reply_markup=main_menu_kb(),
    )
    await callback.answer()


# ── 4. История уведомлений ──────────────────────────────────────────────────

@router.callback_query(F.data == "menu:history")
async def menu_history(callback: CallbackQuery) -> None:
    # Fetch history from DB
    async with db:
        user = await db.get_user(callback.from_user.id)
        if user:
            history = await db.get_user_history(user_id=user["id"])
        else:
            history = []
    
    if not history:
        text = "📜 История уведомлений (пока пусто)"
    else:
        text = "📜 История уведомлений:\n\n"
        for item in history:
            text += f"• {item.get('content', 'N/A')}\n  {item.get('date', '')}\n\n"
    
    await callback.message.edit_text(text, reply_markup=main_menu_kb())
    await callback.answer()


# ── Kafka notification handler ──────────────────────────────────────────────

async def handle_kafka_notification(notification_data: dict) -> None:
    """
    Handle notifications from Kafka topic.
    Formats rich messages based on notification type.
    """
    try:
        telegram_id = notification_data.get("telegram_id") or notification_data.get("chatId")
        title = notification_data.get("title", "Новое уведомление")
        message = notification_data.get("message", "")
        service = notification_data.get("service", "")
        notif_type = notification_data.get("type", "")
        url = notification_data.get("url", "")
        
        if not telegram_id:
            logging.warning(f"No telegram_id in notification: {notification_data}")
            return
        
        text = _format_notification(service, notif_type, title, message, url)
        
        await bot.send_message(
            chat_id=telegram_id,
            text=text,
            parse_mode="HTML",
            disable_web_page_preview=True,
        )
        
        logging.info(f"Notification sent to user {telegram_id}")
        
    except Exception as e:
        logging.error(f"Error handling Kafka notification: {e}", exc_info=True)


def _format_notification(service: str, notif_type: str, title: str, message: str, url: str) -> str:
    """Build a rich formatted notification string."""
    icon = _get_icon(service, notif_type)
    text = f"{icon} <b>{title}</b>\n"
    text += "─" * 20 + "\n"

    if message:
        text += f"{message}\n"

    if url:
        text += f"\n🔗 <a href='{url}'>Открыть на {service.capitalize()}</a>\n"

    svc_label = service.capitalize() if service else "Неизвестный"
    type_label = _type_label(notif_type)
    text += f"\n<i>{svc_label} · {type_label}</i>"

    return text


def _get_icon(service: str, notif_type: str) -> str:
    icons = {
        "auth": "🔑",
        "issue": "🐛",
        "commit": "📝",
        "pull_request": "🔀",
        "branch": "🌿",
        "actions": "⚙️",
        "error": "⚠️",
    }
    return icons.get(notif_type, "🔔")


def _type_label(notif_type: str) -> str:
    labels = {
        "auth": "Авторизация",
        "issue": "Issue",
        "commit": "Commit",
        "pull_request": "Pull Request",
        "branch": "Branch",
        "actions": "GitHub Actions",
        "error": "Ошибка",
    }
    return labels.get(notif_type, notif_type or "Уведомление")


# ── Entry point ──────────────────────────────────────────────────────────────

async def main() -> None:
    dp.include_router(router)
    logging.basicConfig(level=logging.INFO)
    
    # Start bot and Kafka consumer in parallel
    async with asyncio.TaskGroup() as tg:
        tg.create_task(dp.start_polling(bot))
        tg.create_task(kafka_consumer.start(handle_kafka_notification))


if __name__ == "__main__":
    asyncio.run(main())
