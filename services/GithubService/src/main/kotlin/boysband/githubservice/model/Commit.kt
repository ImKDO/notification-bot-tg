package boysband.githubservice.model

import boysband.githubservice.model.utils.Author
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty


@JsonIgnoreProperties(ignoreUnknown = true)
data class Commit(
    val owner: String = "",
    val repo: String = "",

    @JsonProperty("sha")
    val ref: String = "",
    @JsonProperty("html_url")
    val htmlUrl: String = "",
    @JsonProperty("commit")
    val commitInfo: CommitInfo = CommitInfo(),
    @JsonProperty("author")
    val author: Author = Author(),
    @JsonProperty("comments_url")
    val commentsUrl: String = ""
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class CommitInfo(
    @JsonProperty("message")
    val message: String = "",
)