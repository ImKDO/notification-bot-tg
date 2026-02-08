package boysband.githubservice.model.utils

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class Event(
    @JsonProperty("id")
    val id: Long = 0,
    @JsonProperty("event")
    val event: String = "",
    @JsonProperty("actor")
    val actor: Author = Author(),
    @JsonProperty("created_at")
    val createdAt: String = "",
    @JsonProperty("updated_at")
    val updatedAt: String = "",
)