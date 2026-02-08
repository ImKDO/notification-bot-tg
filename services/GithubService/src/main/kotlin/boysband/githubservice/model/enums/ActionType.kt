package boysband.githubservice.model.enums

enum class ActionType {
    COMMIT,
    ISSUE,
    PULL_REQUEST,
    BRANCH,
    GITHUB_ACTIONS;

    companion object {
        fun fromMethodName(name: String): ActionType {
            return valueOf(name.trim().uppercase())
        }
    }
}