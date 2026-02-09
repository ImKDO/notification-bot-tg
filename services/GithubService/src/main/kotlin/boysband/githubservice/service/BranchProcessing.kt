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
        logger.info("[Branch] Processing ${branch.owner}/${branch.repo}:${branch.name} for chatId=$chatId")

        val commits = getCommits(branch, token) ?: emptyList()
        logger.info("[Branch] Fetched ${commits.size} commits from GitHub API")

        val cacheKey = eventStateCache.buildKey(chatId, branch.owner, branch.repo, "branch", branch.name)
        val cachedSha = eventStateCache.getLastCommitSha(cacheKey)
        logger.info("[Branch] Cache key=$cacheKey, lastCommitSha=${cachedSha ?: "<null>"}")

        val newCommits = filterNewCommits(cacheKey, commits)
        logger.info("[Branch] Detected ${newCommits.size} new commits")

        updateCache(cacheKey, commits)

        return BranchEventResponse(
            branch = branch,
            newCommits = newCommits
        )
    }

    fun getCommits(branch: Branch, token: String, perPage: Int = 30): List<Commit>? {
        return try {
            utilsProcessing.baseGithubRequestUrl(branch, "&per_page=$perPage", token)
                ?.onStatus({ it.isError }) { _, response ->
                    logger.error("Error getting branch commits: ${response.statusCode}")
                    throw RuntimeException("GitHub API error ${response.statusCode} for branch ${branch.name}")
                }
                ?.body<List<Commit>>()
                ?.map { it.copy(owner = branch.owner, repo = branch.repo, branch = branch.name) }
        } catch (e: Exception) {
            logger.error("Failed to fetch branch commits for ${branch.owner}/${branch.repo}:${branch.name}", e)
            null
        }
    }

    private fun filterNewCommits(cacheKey: String, commits: List<Commit>): List<Commit> {
        val lastCommitSha = eventStateCache.getLastCommitSha(cacheKey)

        if (lastCommitSha.isNullOrEmpty()) {
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
        if (commits.isEmpty()) {
            val existing = eventStateCache.getLastCommitSha(cacheKey)
            if (existing == null) {
                eventStateCache.setLastCommitSha(cacheKey, "")
            }
            return
        }
        eventStateCache.setLastCommitSha(cacheKey, commits.first().ref)
    }
}
