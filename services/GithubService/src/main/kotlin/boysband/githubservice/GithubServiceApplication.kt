package boysband.githubservice

import lombok.extern.slf4j.Slf4j
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class GithubServiceApplication

fun main(args: Array<String>) {
	val logger: Logger = LoggerFactory.getLogger(GithubServiceApplication::class.java)
	runApplication<GithubServiceApplication>(*args)
}
