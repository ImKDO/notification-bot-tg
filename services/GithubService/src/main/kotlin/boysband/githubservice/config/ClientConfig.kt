package boysband.githubservice.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.web.client.RestClient
@Configuration
class ClientConfig {

    @Bean
    fun baseUrlClient(builder: RestClient.Builder): RestClient {
        return builder
            .baseUrl("https://api.github.com")
            .defaultHeader(HttpHeaders.ACCEPT, "application/vnd.github+json")
            .defaultHeader(HttpHeaders.USER_AGENT, "notification-bot-tg")
            .build()
    }

}