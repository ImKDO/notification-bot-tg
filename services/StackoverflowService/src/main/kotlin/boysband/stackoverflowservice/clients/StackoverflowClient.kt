package boysband.stackoverflowservice.clients

import boysband.stackoverflowservice.dto.Record
import java.time.ZonedDateTime

interface StackoverflowClient {

    suspend fun searchNewComments(url: String, fromDate: ZonedDateTime?): List<Record>
    suspend fun searchNewAnswers(url: String, fromDate: ZonedDateTime?): List<Record>
}