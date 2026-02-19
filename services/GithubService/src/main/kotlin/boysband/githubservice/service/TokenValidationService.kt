package boysband.githubservice.service

import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient

@Service
class TokenValidationService(
    private val baseUrlClient: RestClient,
) {
    fun validateGithubToken(token: String): String? {
        return try {
            val response = baseUrlClient.get()
                .uri("/user")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                .retrieve()
                .body(Map::class.java)

            val login = response?.get("login") as? String
            if (login != null) {
                logger.info("GitHub token validated successfully, username=$login")
                login
            } else {
                logger.warn("GitHub token validation: response has no login field")
                null
            }
        } catch (e: Exception) {
            logger.error("GitHub token validation failed: ${e.message}")
            null
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(TokenValidationService::class.java)
    }
}
