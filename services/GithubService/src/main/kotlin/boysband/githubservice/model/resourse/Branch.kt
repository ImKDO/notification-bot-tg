package boysband.githubservice.model.resourse

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class Branch(
    val owner: String = "",
    val repo: String = "",
    @JsonProperty("name")
    val name: String = "",
    @JsonProperty("commit")
    val commit: BranchCommit = BranchCommit(),
    @JsonProperty("protected")
    val isProtected: Boolean = false,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class BranchCommit(
    @JsonProperty("sha")
    val sha: String = "",
    @JsonProperty("url")
    val url: String = "",
)