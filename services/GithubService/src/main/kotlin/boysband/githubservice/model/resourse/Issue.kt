package boysband.githubservice.model.resourse

import boysband.githubservice.model.utils.Author
import boysband.githubservice.model.utils.Label
import boysband.githubservice.model.utils.Milestone
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class Issue(
    @JsonProperty("id")
    val id: Long = 0,
    @JsonProperty("number")
    val issueNumber: Int,
    val owner: String = "",
    val repo: String = "",
    @JsonProperty("title")
    val title: String = "",
    @JsonProperty("user")
    val author: Author = Author(),
    val body: String = "",
    val state: String = "",
    @JsonProperty("created_at")
    val createdAt: String = "",
    @JsonProperty("updated_at")
    val updatedAt: String = "",
    @JsonProperty("html_url")
    val htmlUrl: String = "",
    @JsonProperty("labels")
    val labels: List<Label> = emptyList(),
    @JsonProperty("milestone")
    val milestone: Milestone? = null,
    @JsonProperty("assignees")
    val assignees: List<Author> = emptyList(),
)