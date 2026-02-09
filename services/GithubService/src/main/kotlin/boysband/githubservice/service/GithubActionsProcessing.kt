package boysband.githubservice.service

import boysband.githubservice.cache.EventStateCache
import boysband.githubservice.model.response.GithubActionsEventResponse
import boysband.githubservice.model.resourse.GithubActions
import boysband.githubservice.model.resourse.WorkflowRun
import boysband.githubservice.model.resourse.WorkflowRunsResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.client.body

@Service
class GithubActionsProcessing(
    private val utilsProcessing: UtilsProcessing,
    private val eventStateCache: EventStateCache
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun processGithubActions(chatId: Long, githubActions: GithubActions, token: String): GithubActionsEventResponse {
        val workflowRuns = getWorkflowRuns(githubActions, token) ?: emptyList()

        val cacheKey = eventStateCache.buildKey(
            chatId,
            githubActions.owner,
            githubActions.repo,
            "actions",
            githubActions.workflowId.ifEmpty { "all" }
        )

        val (newRuns, updatedRuns) = filterNewAndUpdatedRuns(cacheKey, workflowRuns)

        updateCache(cacheKey, workflowRuns)

        return GithubActionsEventResponse(
            githubActions = githubActions,
            newRuns = newRuns,
            updatedRuns = updatedRuns
        )
    }

    fun getWorkflowRuns(githubActions: GithubActions, token: String, perPage: Int = 30): List<WorkflowRun>? {
        return try {
            utilsProcessing.baseGithubRequestUrl(githubActions, "?per_page=$perPage", token)
                ?.onStatus({ it.isError }) { _, response ->
                    throw RuntimeException("GitHub API error ${response.statusCode} for workflow runs")
                }
                ?.body<WorkflowRunsResponse>()
                ?.workflowRuns
        } catch (e: Exception) {
            logger.error("Failed to fetch workflow runs for ${githubActions.owner}/${githubActions.repo}", e)
            null
        }
    }

    private fun filterNewAndUpdatedRuns(cacheKey: String, runs: List<WorkflowRun>): Pair<List<WorkflowRun>, List<WorkflowRun>> {
        val lastRunId = eventStateCache.getLastWorkflowRunId(cacheKey)

        if (lastRunId == null) {
            return Pair(emptyList(), emptyList())
        }

        val newRuns = mutableListOf<WorkflowRun>()
        val updatedRuns = mutableListOf<WorkflowRun>()

        for (run in runs) {
            if (run.id > lastRunId) {
                newRuns.add(run)
            } else {
                // Проверяем, изменился ли статус (например, in_progress -> completed)
                val cachedStatus = eventStateCache.getWorkflowRunStatus("$cacheKey:run:${run.id}")
                val currentStatus = "${run.status}:${run.conclusion ?: "null"}"
                if (cachedStatus != null && cachedStatus != currentStatus) {
                    updatedRuns.add(run)
                }
            }
        }

        return Pair(newRuns, updatedRuns)
    }

    private fun updateCache(cacheKey: String, runs: List<WorkflowRun>) {
        if (runs.isNotEmpty()) {
            val maxRunId = runs.maxByOrNull { it.id }!!.id
            eventStateCache.setLastWorkflowRunId(cacheKey, maxRunId)
            runs.forEach { run ->
                val status = "${run.status}:${run.conclusion ?: "null"}"
                eventStateCache.setWorkflowRunStatus("$cacheKey:run:${run.id}", status)
            }
        } else if (eventStateCache.getLastWorkflowRunId(cacheKey) == null) {
            eventStateCache.setLastWorkflowRunId(cacheKey, 0L)
        }
    }
}
