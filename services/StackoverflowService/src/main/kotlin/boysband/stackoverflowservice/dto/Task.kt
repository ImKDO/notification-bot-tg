package boysband.stackoverflowservice.dto

import java.time.ZonedDateTime

data class Task(
    val actionId: Int,
    val chatId: Long,
    val link: String,
    val type: TaskType,
    val previousDate: ZonedDateTime,
) {
    sealed class TaskType{
        data object NEW_ANSWER : TaskType()
        data object NEW_COMMENT : TaskType()
    }
}