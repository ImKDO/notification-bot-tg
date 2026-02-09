
package boysband.coreservice.service

import boysband.coreservice.client.DbServiceClient
import boysband.coreservice.dto.ActionDto
import boysband.coreservice.dto.Task
import boysband.coreservice.kafka.TaskProducer
import org.springframework.stereotype.Service

@Service
class TaskCreatorService(
    private val client: DbServiceClient,
    private val sender: TaskProducer
) {

    suspend fun giveNewTasks() {
        val actions = getActions()
        println(actions)
        val tasks = routeTasks(actions)
        tasks.forEach { (topic, task) ->
            sender.sendUpdate(task, topic)
        }
    }

    private suspend fun getActions(): List<ActionDto> {
        println("Получаем действия из БД...")
        return client.getActions()
    }

    private fun routeTasks(actions: List<ActionDto>): List<Pair<String, Task>> {
        val result = mutableListOf<Pair<String, Task>>()
        actions.forEach { action ->
            val task = when (action.service.name.lowercase()) {
                "stackoverflow" -> {
                    Task.StackOverflowTask(
                        link = action.query,
                        actionId = action.id,
                        type = when(action.method?.name) {
                            "new_comment" -> Task.StackOverflowTask.TaskType.NEW_COMMENT
                            "new_answer" -> Task.StackOverflowTask.TaskType.NEW_ANSWER
                            else -> throw IllegalArgumentException("Unknown method name: ${action.method?.name}")
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

                else -> throw IllegalArgumentException("Unknown service name: ${action.service.name}")
            }
            result.add(Pair(when (action.service.name.lowercase()) {
                "stackoverflow" -> "stackoverflow"
                "github" -> "github_request"
                else -> throw IllegalArgumentException("Unknown service name: ${action.service.name}")
            }, task))
        }

        return result
    }
}