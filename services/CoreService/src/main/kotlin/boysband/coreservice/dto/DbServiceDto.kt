package boysband.coreservice.dto

import java.time.LocalDateTime
import java.time.ZonedDateTime

data class ActionDto(
    val id: Int = 0,
    val method: MethodDto? = null,
    val token: TokenDto? = null,
    val user: UserDto? = null,
    val service: ServiceDto,
    val describe: String = "",
    val query: String = "",
    val date: LocalDateTime? = null,
    val lastCheckDate: ZonedDateTime
)

data class MethodDto(
    val id: Int = 0,
    val serviceId: Int = 0,
    val name: String = "",
    val describe: String = "",
)

data class TokenDto(
    val id: Int = 0,
    val value: String = "",
    val user: UserDto? = null,
)

data class UserDto(
    val id: Int = 0,
    val idTgChat: Long = 0,
    val date: LocalDateTime? = null,
)

data class ServiceDto(
    val id: Int = 0,
    val link: String = "",
    val name: String = ""
)