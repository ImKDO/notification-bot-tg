package boysband.githubservice.kafka

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Service

@Service
class Consumer {
    companion object {
        val log: Logger = LoggerFactory.getLogger(this::class.java)
    }
    @KafkaListener(topics = ["github_request"], groupId = "github_request_group")
    fun listen(msg: String) {
        log.info("Github Consumer: $msg")

    }


}