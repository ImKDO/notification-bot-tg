package boysband.coreservice.scheduler

import boysband.coreservice.service.TaskCreatorService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component


@Component
class TaskCreateScheduler(
    private val handler: TaskCreatorService
) {
    private val scope = CoroutineScope(Dispatchers.IO)

    @Scheduled(fixedDelayString = $$"${scheduler.check-links}")
    fun run() {
        println("start scheduler")
        scope.launch {
            handler.giveNewTasks()
        }
    }
}