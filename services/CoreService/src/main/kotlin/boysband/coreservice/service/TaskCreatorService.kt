
package boysband.coreservice.service

import boysband.coreservice.client.DbServiceClient
import boysband.coreservice.dto.ActionDto
import boysband.coreservice.dto.Task
import boysband.coreservice.kafka.TaskProducer
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class TaskCreatorService(
    private val client: DbServiceClient,
    private val sender: TaskProducer
) {

    suspend fun giveNewTasks() {
        try {
            val actions = getActions()
            logger.info("Fetched ${actions.size} actions from DB")
            val tasks = routeTasks(actions)
            tasks.forEach { (topic, task) ->
                try {
                    sender.sendUpdate(task, topic)
                } catch (e: Exception) {
                    logger.error("Failed to send task to topic $topic: ${e.message}", e)
                }
            }
        } catch (e: Exception) {
            logger.error("Error in giveNewTasks: ${e.message}", e)
        }
    }

    private suspend fun getActions(): List<ActionDto> {
        logger.info("Получаем действия из БД...")
        return client.getActions()
    }

    private fun routeTasks(actions: List<ActionDto>): List<Pair<String, Task>> {
        val result = mutableListOf<Pair<String, Task>>()
        actions.forEach { action ->
            try {
                val task = when (action.service.name.lowercase()) {
                    "stackoverflow" -> {
                        Task.StackOverflowTask(
                            link = action.query,
                            actionId = action.id,
                            type = when (action.method?.name?.lowercase()) {
                                "new_comment" -> Task.StackOverflowTask.TaskType.NEW_COMMENT
                                "new_answer" -> Task.StackOverflowTask.TaskType.NEW_ANSWER
                                else -> {
                                    logger.warn("Skipping action ${action.id}: unknown SO method '${action.method?.name}'")
                                    return@forEach
                                }
                            },
                            previousDate = action.lastCheckDate,
                            chatId = action.user?.idTgChat ?: 0
                        )
                    }
                    "github" -> {
                        Task.GithubTask(
                            id = action.id,
                            query = action.query,
                            token = action.token,
                            user = action.user,
                            service = action.service,
                            method = action.method,
                            describe = action.describe,
                            date = action.date.toString()
                        )
                    }
                    else -> {
                        logger.warn("Skipping action ${action.id}: unknown service '${action.service.name}'")
                        return@forEach
                    }
                }
                val topic = when (action.service.name.lowercase()) {
                    "stackoverflow" -> "stackoverflow"
                    "github" -> "github_request"
                    else -> return@forEach
                }
                result.add(Pair(topic, task))
            } catch (e: Exception) {
                logger.error("Error routing action ${action.id}: ${e.message}", e)
            }
        }
        return result
    }

    companion object {
        private val logger = LoggerFactory.getLogger(TaskCreatorService::class.java)
    }
}