package boysband.githubservice.model.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

/**
 * DTO, зеркалирующий структуру entity Action из DBService.
 * Используется как входящее сообщение из Kafka.
 *
 * method.name — тип действия (COMMIT, ISSUE, PULL_REQUEST, BRANCH, GITHUB_ACTIONS)
 * token.value — GitHub API token
 * user.idTgChat — Telegram chat ID пользователя
 * service.link — базовый URL сервиса
 * query — конкретный GitHub URL для отслеживания
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class ActionDto(
    val id: Int = 0,
    val method: MethodDto = MethodDto(),
    val token: TokenDto = TokenDto(),
    val user: UserDto = UserDto(),
    val service: ServiceDto = ServiceDto(),
    val describe: String = "",
    val query: String = "",
    val date: String = ""
)
