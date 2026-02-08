package boysband.githubservice.model.resourse

import boysband.githubservice.model.utils.Author
import boysband.githubservice.model.utils.Label
import boysband.githubservice.model.utils.Milestone
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class PullRequest(
    @JsonProperty("id")
    val id: Long = 0,
    @JsonProperty("number")
    val prNumber: Int = 0,
    val owner: String = "",
    val repo: String = "",
    @JsonProperty("title")
    val title: String = "",
    @JsonProperty("body")
    val body: String = "",
    @JsonProperty("state")
    val state: String = "",
    @JsonProperty("user")
    val author: Author = Author(),
    @JsonProperty("html_url")
    val htmlUrl: String = "",
    @JsonProperty("created_at")
    val createdAt: String = "",
    @JsonProperty("updated_at")
    val updatedAt: String = "",
    @JsonProperty("merged_at")
    val mergedAt: String? = null,
    @JsonProperty("head")
    val head: PullRequestBranch = PullRequestBranch(),
    @JsonProperty("base")
    val base: PullRequestBranch = PullRequestBranch(),
    @JsonProperty("labels")
    val labels: List<Label> = emptyList(),
    @JsonProperty("milestone")
    val milestone: Milestone? = null,
    @JsonProperty("assignees")
    val assignees: List<Author> = emptyList(),
    @JsonProperty("requested_reviewers")
    val requestedReviewers: List<Author> = emptyList(),
    @JsonProperty("draft")
    val draft: Boolean = false,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class PullRequestBranch(
    @JsonProperty("ref")
    val ref: String = "",
    @JsonProperty("sha")
    val sha: String = "",
)
