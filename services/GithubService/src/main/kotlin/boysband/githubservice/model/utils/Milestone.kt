package boysband.githubservice.model.utils

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class Milestone(
    @JsonProperty("id")
    val id: Long = 0,
    @JsonProperty("number")
    val number: Int = 0,
    @JsonProperty("title")
    val title: String = "",
    @JsonProperty("description")
    val description: String? = null,
    @JsonProperty("state")
    val state: String = "",
    @JsonProperty("due_on")
    val dueOn: String? = null,
    @JsonProperty("html_url")
    val htmlUrl: String = "",
)
