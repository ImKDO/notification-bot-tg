package boysband.githubservice.model

import boysband.githubservice.model.utils.Author
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class GithubActions(
    val owner: String = "",
    val repo: String = "",
    val workflowId: String = "", // can be workflow file name or id
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class WorkflowRunsResponse(
    @JsonProperty("total_count")
    val totalCount: Int = 0,
    @JsonProperty("workflow_runs")
    val workflowRuns: List<WorkflowRun> = emptyList(),
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class WorkflowRun(
    @JsonProperty("id")
    val id: Long = 0,
    @JsonProperty("name")
    val name: String = "",
    @JsonProperty("head_branch")
    val headBranch: String = "",
    @JsonProperty("head_sha")
    val headSha: String = "",
    @JsonProperty("status")
    val status: String = "",
    @JsonProperty("conclusion")
    val conclusion: String? = null,
    @JsonProperty("html_url")
    val htmlUrl: String = "",
    @JsonProperty("created_at")
    val createdAt: String = "",
    @JsonProperty("updated_at")
    val updatedAt: String = "",
    @JsonProperty("run_number")
    val runNumber: Int = 0,
    @JsonProperty("event")
    val event: String = "",
    @JsonProperty("actor")
    val actor: Author = Author(),
)
