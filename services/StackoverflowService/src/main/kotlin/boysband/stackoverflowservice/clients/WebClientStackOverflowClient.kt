package boysband.stackoverflowservice.clients

import boysband.stackoverflowservice.dto.Record
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

@Component("webClientRealization")
class WebClientStackOverflowClient(
    private val client: WebClient,
    private val mapper: ObjectMapper
) : StackoverflowClient {


    override suspend fun searchNewComments(url: String, fromDate: ZonedDateTime?): List<Record> {
        val fromDate = if (fromDate != null) fromDate.toEpochSecond() else 0
        val id = """/questions/(\d+)/""".toRegex().find(url)?.groups?.get(1)?.value

        val request = client
            .method(HttpMethod.GET)
            .uri { uriBuilder ->
                uriBuilder
                    .path("/questions/{id}/comments")
                    .queryParam("site", "{site}")
                    .queryParam("order", "{order}")
                    .queryParam("sort", "{sort}")
                    .queryParam("fromdate", fromDate)
                    .queryParam("filter", FILTERS["comments"])
                    .build(id)
            }

        try{
            val body = mapper.readTree(request.retrieve().awaitBody<String>())

            val result = ArrayList<Record>()

            body["items"].forEach { item ->
                item["answers"]?.forEach {answer ->
                    result.add(
                        Record(
                            author = answer["owner"]["display_name"].asText(),
                            text = answer["body"].asText(),
                            creationDate = ZonedDateTime.ofInstant(
                                Instant.ofEpochSecond(answer["creation_date"].asLong()),
                                ZoneId.of("UTC")
                            ),
                        )
                    )
                }
            }

            return result
        }
        catch (ex: Exception){
            println("Exception ${ex.message}")
            return emptyList()
        }
    }

    override suspend fun searchNewAnswers(url: String, fromDate: ZonedDateTime?): List<Record> {
        val fromDate = if (fromDate != null) fromDate.toEpochSecond() else 0
        val id = """/questions/(\d+)/""".toRegex().find(url)?.groups?.get(1)?.value

        val request = client
            .method(HttpMethod.GET)
            .uri { uriBuilder ->
                uriBuilder
                    .path("/questions/{id}/comments")
                    .queryParam("site", "{site}")
                    .queryParam("order", "{order}")
                    .queryParam("sort", "{sort}")
                    .queryParam("fromdate", fromDate)
                    .queryParam("filter", FILTERS["comments"])
                    .build(id)
            }

        try{
            val body = mapper.readTree(request.retrieve().awaitBody<String>())

            val result = ArrayList<Record>()

            body["items"].forEach { item ->
                item["comments"]?.forEach {answer ->
                    result.add(
                        Record(
                            author = answer["owner"]["display_name"].asText(),
                            text = answer["body"].asText(),
                            creationDate = ZonedDateTime.ofInstant(
                                Instant.ofEpochSecond(answer["creation_date"].asLong()),
                                ZoneId.of("UTC")
                            ),
                        )
                    )
                }
            }

            return result
        }
        catch (ex: Exception){
            println("Exception ${ex.message}")
            return emptyList()
        }
    }

    companion object{
        val FILTERS: Map<String, String> = mapOf(
            "comments" to "!T3AudphlMFjTE*ozcm",
            "answers" to "!*Mg4Pjfm.dOZh)cH"
        )
    }
}