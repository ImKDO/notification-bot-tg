package boysband.githubservice.model.resourse

import boysband.githubservice.model.utils.Author
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty


@JsonIgnoreProperties(ignoreUnknown = true)
data class Commit(
    val owner: String = "",
    val repo: String = "",
    val branch: String = "",

    @JsonProperty("sha")
    val ref: String = "",
    @JsonProperty("html_url")
    val htmlUrl: String = "",
    @JsonProperty("commit")
    val commitInfo: CommitInfo = CommitInfo(),
    @JsonProperty("author")
    val author: Author? = null,
    @JsonProperty("comments_url")
    val commentsUrl: String = ""
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class CommitInfo(
    @JsonProperty("message")
    val message: String = "",
    @JsonProperty("author")
    val authorInfo: CommitAuthorInfo = CommitAuthorInfo(),
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class CommitAuthorInfo(
    @JsonProperty("name")
    val name: String = "",
    @JsonProperty("email")
    val email: String = "",
    @JsonProperty("date")
    val date: String = "",
)