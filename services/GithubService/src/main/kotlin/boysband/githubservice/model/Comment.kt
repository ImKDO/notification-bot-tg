package boysband.githubservice.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class Comment(
    @JsonProperty("user")
    val author: Author = Author(),
    @JsonProperty("body")
    val body: String = "",
    @JsonProperty("html_url")
    val link: String = "",
    @JsonProperty("created_at")
    val createdAt: String = "",
    @JsonProperty("updated_at")
    val updatedAt: String = "",
)

