package boysband.coreservice.client

import boysband.coreservice.dto.ActionDto
import kotlinx.coroutines.reactive.awaitSingle
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBodilessEntity

@Component
class HttpDbServiceClient(
    private val client: WebClient,
): DbServiceClient {
    override suspend fun getActions(): List<ActionDto> {
        return client.get()
            .uri("/actions")
            .retrieve()
            .bodyToFlux(ActionDto::class.java)
            .collectList()
            .awaitSingle()
    }

    override suspend fun updateAction(id: Int, action: ActionDto) {
        client.put()
            .uri("/actions/{id}", id)
            .bodyValue(action)
            .retrieve()
            .awaitBodilessEntity()
    }

    override suspend fun deleteAction(id: Int) {
        client.delete()
            .uri("/actions/{id}", id)
            .retrieve()
            .awaitBodilessEntity()
    }
}