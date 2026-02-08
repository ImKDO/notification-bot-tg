package boysband.githubservice.kafka

import boysband.githubservice.model.UserRequest
import boysband.githubservice.service.GithubProcessing
import org.apache.catalina.User
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Service

@Service
class Consumer(
    private val githubProcessing: GithubProcessing
) {
    companion object {
        val log: Logger = LoggerFactory.getLogger(this::class.java)
    }

    @KafkaListener(topics = ["github_request"], groupId = "github_request_group")
    fun listen(userRequest: UserRequest) {
        log.info("Github Consumer get: ${userRequest}")

        githubProcessing.getResponse(userRequest)

    }


}