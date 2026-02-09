package boysband.stackoverflowservice.service

import boysband.stackoverflowservice.clients.StackoverflowClient
import boysband.stackoverflowservice.dto.Task
import boysband.stackoverflowservice.dto.Update
import boysband.stackoverflowservice.kafka.UpdateProducer
import org.apache.kafka.clients.producer.internals.Sender
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import java.time.ZonedDateTime

@Service
class TaskHandler(
    private val sender: UpdateProducer,
    @param:Qualifier("retryingRealization")
    private val client: StackoverflowClient,
) {
    open suspend fun handleTask(task: Task){
        val updates = when(task.type) {
            Task.TaskType.NEW_COMMENT -> client.searchNewComments(task.link, task.previousDate)
            Task.TaskType.NEW_ANSWER -> client.searchNewAnswers(task.link, task.previousDate)
        }
        updates.forEach {
            val update = Update(
                author = it.author,
                text = it.text,
                link = task.link,
                type = when(task.type) {
                    Task.TaskType.NEW_COMMENT -> Update.Type.COMMENTS
                    Task.TaskType.NEW_ANSWER -> Update.Type.ANSWERS
                },
                creationDate = it.creationDate,
                actionId = task.actionId,
                chatId = task.chatId
            )
            sender.sendUpdate(update)
        }
    }
}