package boysband.githubservice.model

import boysband.githubservice.model.enums.ActionType


data class UserRequest(
    val chatId: Long,
    val action: Action,
    val dataForRequest: HashMap<String, String>,
)

data class Action (
    val name: ActionType,
    val token: String,
)