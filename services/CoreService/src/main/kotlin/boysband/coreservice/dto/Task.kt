package boysband.coreservice.dto

import java.time.ZonedDateTime


sealed class Task{
    data class StackOverflowTask(
        val actionId: Int,
        val chatId: Long,
        val link: String,
        val type: TaskType,
        val previousDate: ZonedDateTime
    ): Task() {
        sealed class TaskType{
            data object NEW_ANSWER : TaskType()
            data object NEW_COMMENT : TaskType()
        }
    }

    data class GithubTask(
        val id: Int = 0,
        val method: MethodDto?,
        val token: TokenDto?,
        val user: UserDto?,
        val service: ServiceDto?,
        val describe: String,
        val query: String,
        val date: String = ""
    ): Task()
}

//data class StackOverflowTask(
//    val id: Long,
//    val link: String,
//    val type: TaskType,
//    val previousDate: ZonedDateTime
//) {
//    sealed class TaskType{
//        data object NEW_ANSWER : TaskType()
//        data object NEW_COMMENT : TaskType()
//    }
//}

//data class GithubTask(
//
//){
//
//}