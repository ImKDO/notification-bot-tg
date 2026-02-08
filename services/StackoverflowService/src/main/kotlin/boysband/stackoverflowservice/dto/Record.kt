package boysband.stackoverflowservice.dto

import java.time.ZonedDateTime

data class Record(
    val author: String,
    val text: String,
    val creationDate: ZonedDateTime
) {
    enum class Type{
        ANSWER, COMMENT
    }
}