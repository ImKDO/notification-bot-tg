import asyncio
import logging
import os

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
        [InlineKeyboardButton(text="2. Настроить уведомления", callback_data="menu:notifications")],
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
        [InlineKeyboardButton(text="Issue", callback_data="sub:issue")],
        [InlineKeyboardButton(text="Pull Request", callback_data="sub:pull_request")],
        [InlineKeyboardButton(text="Commit", callback_data="sub:commit")],
        [InlineKeyboardButton(text="Github Actions", callback_data="sub:actions")],
        [InlineKeyboardButton(text="Branch", callback_data="sub:branch")],
        [InlineKeyboardButton(text="⬅️ Назад", callback_data="back:main")],
    ])


def notification_period_kb() -> InlineKeyboardMarkup:
    return InlineKeyboardMarkup(inline_keyboard=[
        [InlineKeyboardButton(text="5 секунд", callback_data="period:5s")],
        [InlineKeyboardButton(text="5 минут", callback_data="period:5m")],
        [InlineKeyboardButton(text="15 минут", callback_data="period:15m")],
        [InlineKeyboardButton(text="⬅️ Назад", callback_data="back:main")],
    ])


# ── Handlers ─────────────────────────────────────────────────────────────────

@router.message(CommandStart())
async def cmd_start(message: Message, state: FSMContext) -> None:
    await state.clear()
    # Register user in DB
    async with db:
        await db.create_user(
            telegram_id=message.from_user.id,
        )
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
    
    # Save token to DB
    async with db:
        await db.create_token(
            telegram_id=message.from_user.id,
            token_value=token,
        )
    
    await state.update_data(token=token)
    await message.answer(
        "Токен сохранён ✅\n\nНа что подписаться?",
        reply_markup=subscribe_kb(),
    )
    await state.set_state(None)


@router.callback_query(F.data.startswith("sub:"))
async def subscribe_select(callback: CallbackQuery, state: FSMContext) -> None:
    sub_type = callback.data.split(":")[1]
    await state.update_data(sub_type=sub_type)
    label = {
        "issue": "Issue",
        "pull_request": "Pull Request",
        "commit": "Commit",
        "actions": "Github Actions",
        "branch": "Branch",
    }.get(sub_type, sub_type)
    await callback.message.edit_text(
        f"Вы выбрали: {label}\n\n"
        "Пришлите ссылку на ваш выбранный на предыдущем шаге ресурс:"
    )
    await state.set_state(AuthStates.waiting_for_resource_link)
    await callback.answer()


@router.message(AuthStates.waiting_for_resource_link)
async def process_resource_link(message: Message, state: FSMContext) -> None:
    link = message.text.strip()
    data = await state.get_data()
    
    # Note: This is a simplified version. In real implementation,
    # you need to get service_id, method_id, and token_id from the database
    # based on the user's choices. For now, we'll just acknowledge the subscription.
    
    await message.answer(
        f"Подписка оформлена ✅\n\n"
        f"Сервис: {data.get('service', '').capitalize()}\n"
        f"Тип: {data.get('sub_type', '')}\n"
        f"Ресурс: {link}\n\n"
        "Возвращаемся в главное меню.\n\n"
        "⚠️ Примечание: Для полной реализации требуется настройка service_id, method_id и token_id.",
        reply_markup=main_menu_kb(),
    )
    await state.clear()


# ── 2. Настроить уведомления ────────────────────────────────────────────────

@router.callback_query(F.data == "menu:notifications")
async def menu_notifications(callback: CallbackQuery) -> None:
    await callback.message.edit_text(
        "Выберите период получения уведомлений:",
        reply_markup=notification_period_kb(),
    )
    await callback.answer()


@router.callback_query(F.data.startswith("period:"))
async def period_select(callback: CallbackQuery) -> None:
    period = callback.data.split(":")[1]
    label = {"5s": "5 секунд", "5m": "5 минут", "15m": "15 минут"}.get(period, period)
    period_seconds = {"5s": 5, "5m": 300, "15m": 900}.get(period, 300)
    
    # Note: Notification period setting is not implemented in DBService API
    # This would require adding custom endpoint or storing in User entity
    
    await callback.message.edit_text(
        f"Период уведомлений установлен: {label} ✅\n\n"
        "⚠️ Примечание: Функция сохранения периода требует расширения DBService API.",
        reply_markup=main_menu_kb(),
    )
    await callback.answer()


# ── 3. Настроить теги ───────────────────────────────────────────────────────

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
    Expected notification format:
    {
        "telegram_id": 123456,
        "title": "Notification title",
        "message": "Notification message",
        "service": "github",
        "type": "issue",
        "url": "https://github.com/..."
    }
    """
    try:
        telegram_id = notification_data.get("telegram_id")
        title = notification_data.get("title", "Новое уведомление")
        message = notification_data.get("message", "")
        service = notification_data.get("service", "")
        notif_type = notification_data.get("type", "")
        url = notification_data.get("url", "")
        
        if not telegram_id:
            logging.warning(f"No telegram_id in notification: {notification_data}")
            return
        
        # Format notification message
        text = f"🔔 <b>{title}</b>\n\n"
        if message:
            text += f"{message}\n\n"
        if service:
            text += f"📌 Сервис: {service.capitalize()}\n"
        if notif_type:
            text += f"📋 Тип: {notif_type}\n"
        if url:
            text += f"🔗 <a href='{url}'>Открыть</a>"
        
        await bot.send_message(
            chat_id=telegram_id,
            text=text,
            parse_mode="HTML",
            disable_web_page_preview=True,
        )
        
        logging.info(f"Notification sent to user {telegram_id}")
        
    except Exception as e:
        logging.error(f"Error handling Kafka notification: {e}", exc_info=True)


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
