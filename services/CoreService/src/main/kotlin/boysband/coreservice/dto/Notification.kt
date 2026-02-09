package boysband.coreservice.dto

data class Notification(
    val chatId: Long,
    val title: String,
    val message: String,
    val service: String,
    val type: String,
    val url: String,
) {
}