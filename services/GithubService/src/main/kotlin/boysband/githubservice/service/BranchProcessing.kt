package boysband.githubservice.service

import boysband.githubservice.cache.EventStateCache
import boysband.githubservice.model.response.BranchEventResponse
import boysband.githubservice.model.resourse.Branch
import boysband.githubservice.model.resourse.Commit
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.client.body

@Service
class BranchProcessing(
    private val utilsProcessing: UtilsProcessing,
    private val eventStateCache: EventStateCache
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun processBranch(chatId: Long, branch: Branch, token: String): BranchEventResponse {
        val commits = getCommits(branch, token) ?: emptyList()

        val cacheKey = eventStateCache.buildKey(chatId, branch.owner, branch.repo, "branch", branch.name)

        val newCommits = filterNewCommits(cacheKey, commits)

        updateCache(cacheKey, commits)

        return BranchEventResponse(
            branch = branch,
            newCommits = newCommits
        )
    }

    fun getCommits(branch: Branch, token: String, perPage: Int = 30): List<Commit>? {
        return utilsProcessing.baseGithubRequestUrl(branch, "&per_page=$perPage", token)
            ?.onStatus({ it.isError }) { _, response ->
                logger.error("Error getting branch commits: ${response.statusCode}")
            }
            ?.body<List<Commit>>()
            ?.map { it.copy(owner = branch.owner, repo = branch.repo, branch = branch.name) }
    }

    private fun filterNewCommits(cacheKey: String, commits: List<Commit>): List<Commit> {
        val lastCommitSha = eventStateCache.getLastCommitSha(cacheKey)

        if (lastCommitSha == null) {
            return emptyList()
        }

        val newCommits = mutableListOf<Commit>()
        for (commit in commits) {
            if (commit.ref == lastCommitSha) {
                break
            }
            newCommits.add(commit)
        }

        return newCommits
    }

    private fun updateCache(cacheKey: String, commits: List<Commit>) {
        commits.firstOrNull()?.let {
            eventStateCache.setLastCommitSha(cacheKey, it.ref)
        }
    }
}
