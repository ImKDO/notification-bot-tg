package boysband.githubservice.model.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class ServiceDto(
    val id: Int = 0,
    val link: String = ""
)
