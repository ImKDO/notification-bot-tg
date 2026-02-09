package boysband.githubservice.model.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class UserDto(
    val id: Int = 0,
    val idTgChat: Long = 0
)
