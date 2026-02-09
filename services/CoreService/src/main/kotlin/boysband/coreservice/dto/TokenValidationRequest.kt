package boysband.coreservice.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class TokenValidationRequest(
    val telegramId: Long = 0,
    val token: String = "",
    val service: String = "",
)
