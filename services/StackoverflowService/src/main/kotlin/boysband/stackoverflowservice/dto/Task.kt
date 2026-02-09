package boysband.stackoverflowservice.dto

import java.time.ZonedDateTime

data class Task(
    val actionId: Int = 0,
    val chatId: Long = 0,
    val link: String = "",
    val type: TaskType = TaskType.NEW_ANSWER,
    val previousDate: ZonedDateTime = ZonedDateTime.now(),
) {
    enum class TaskType {
        NEW_ANSWER, NEW_COMMENT
    }
}