package boysband.githubservice.model.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

/**
 * Зеркалирует entity Token из DBService.
 * Token привязан к User через id_tg_chat.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class TokenDto(
    val id: Int = 0,
    val value: String = "",
    val user: UserDto = UserDto()
)
