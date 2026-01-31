package boysband.githubservice.service

import boysband.githubservice.model.Action
import boysband.githubservice.model.utils.Author
import boysband.githubservice.model.Branch
import boysband.githubservice.model.Commit
import boysband.githubservice.model.utils.Event
import boysband.githubservice.model.GithubActions
import boysband.githubservice.model.Issue
import boysband.githubservice.model.PullRequest
import boysband.githubservice.model.enums.ActionType
import com.fasterxml.jackson.databind.JsonNode
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.body

@Component
class UtilsProcessing(
    private val baseUrlClient: RestClient
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun baseGithubRequestUrl(
        objectForRequest: Any,
        endpoint: String,
        token: String,
    ): RestClient.ResponseSpec? {

        when (objectForRequest) {
            is Issue -> {
                val issueNumber = objectForRequest.issueNumber
                val owner = objectForRequest.owner
                val repo = objectForRequest.repo

                return baseUrlClient.get()
                    .uri("/repos/$owner/$repo/issues/$issueNumber$endpoint")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                    .retrieve()
            }

            is Commit -> {
                val owner = objectForRequest.owner
                val repo = objectForRequest.repo
                val ref = objectForRequest.ref

                return baseUrlClient.get()
                    .uri("/repos/$owner/$repo/commits/$ref$endpoint")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                    .retrieve()
            }

            is PullRequest -> {

            }

            is GithubActions -> {

            }

            is Branch -> {

            }

            else -> {

            }
        }
        return null
    }
     fun parseGithubUrl(typeAction: ActionType, url: String): Any {
         val regex = when (typeAction) {
             ActionType.ISSUE -> {
                 Regex("""github\.com/([^/]+)/([^/]+)/issues/(\d+)""")
             }

             ActionType.COMMIT -> {
                 Regex("""github\.com/([^/]+)/([^/]+)/commit/([0-9a-fA-F]+)""")
             }

             ActionType.PULL_REQUEST -> {
                 Regex("""github\.com/([^/]+)/([^/]+)/pull/(\d+)""")
             }
             else -> {
                 return ""
             }
         }

         val matchResult = regex.find(url)

        if (matchResult != null) {
            val (owner, repo, ref) = matchResult.destructured

            return when (typeAction) {
                ActionType.ISSUE -> {
                    Issue(
                        owner = owner,
                        repo = repo,
                        issueNumber = ref.toInt(),
                    )
                }
                ActionType.COMMIT -> {
                    Commit(
                        owner = owner,
                        repo = repo,
                        ref = ref
                    )
                }
//                ActionType.EVENT -> {
//                    Event(
//
//                    )
//                }
                else -> {
                    "Unknown"
                }
//                ActionType.PULL_REQUEST -> {
//                    PullRequest(
//
//                    )
//                }

            }
        } else {
            return Issue(
                issueNumber = 0,
                owner = "",
                repo = ""
            )
        }
    }
}