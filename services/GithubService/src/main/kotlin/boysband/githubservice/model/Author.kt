package boysband.githubservice.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class Author (
    @JsonProperty("login")
    val name: String,
    @JsonProperty("html_url")
    val linkOnAuthor: String,
)
