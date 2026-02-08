package boysband.githubservice.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class Issue(
    @JsonProperty("number")
    val issueNumber: Int,
    val owner: String = "",
    val repo: String = "",
    @JsonProperty("user")
    val author: Author = Author(),
    val body: String = "",
)
