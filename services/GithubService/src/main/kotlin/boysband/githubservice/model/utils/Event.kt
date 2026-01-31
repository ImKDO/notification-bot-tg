package boysband.githubservice.model.utils

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class Event(
    val event: String = "",
    val actor: Author = Author(),
    val created_at: String = "",
)