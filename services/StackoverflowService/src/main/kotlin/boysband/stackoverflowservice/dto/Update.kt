package boysband.stackoverflowservice.dto

import java.time.ZonedDateTime

data class Update(
    val author: String,
    val text: String,
    val creationDate: ZonedDateTime,
    val type: Type,
    val link: String,
    val actionId: Int,
    val chatId: Long
) {

    sealed class Type{
        data object ANSWERS : Type()
        data object COMMENTS : Type()
    }
}