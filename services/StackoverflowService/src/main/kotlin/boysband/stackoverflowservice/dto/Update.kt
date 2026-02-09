package boysband.stackoverflowservice.dto

import java.time.ZonedDateTime

data class Update(
    val author: String = "",
    val text: String = "",
    val creationDate: ZonedDateTime = ZonedDateTime.now(),
    val type: Type = Type.ANSWERS,
    val link: String = "",
    val actionId: Int = 0,
    val chatId: Long = 0
) {
    enum class Type {
        ANSWERS, COMMENTS
    }
}