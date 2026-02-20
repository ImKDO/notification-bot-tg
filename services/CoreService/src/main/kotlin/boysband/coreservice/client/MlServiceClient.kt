package boysband.coreservice.client

interface MlServiceClient {
    suspend fun summarize(notifications: List<String>, maxTokens: Int = 200): String?
}
