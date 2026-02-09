package boysband.coreservice.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class TokenValidationResult(
    val telegramId: Long = 0,
    val token: String = "",
    val valid: Boolean = false,
    val username: String = "",
    val service: String = "github",
)
