package boysband.stackoverflowservice.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType

import org.springframework.web.reactive.function.client.WebClient

@Configuration
class WebClientConfig {

    @Bean
    fun webClient(builder: WebClient.Builder): WebClient =
        builder
            .baseUrl("https://api.stackexchange.com/2.3")
            .defaultUriVariables(mapOf(
                "site" to "stackoverflow",
                "order" to "desc",
                "sort" to "creation",
            ))
            .defaultHeader(
                HttpHeaders.CONTENT_TYPE,
                MediaType.APPLICATION_JSON_VALUE)
            .defaultHeaders { headers ->
                headers.set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                headers.set(HttpHeaders.USER_AGENT, "LinkTracker/1.0")
            }
            .build()
}