package boysband.githubservice.model

import boysband.githubservice.model.enums.ActionType

data class UserRequest(
    val chatId: Long,
    val action: Action,
    val link: String,
)

data class Action (
    val name: ActionType,
    val token: String,
)