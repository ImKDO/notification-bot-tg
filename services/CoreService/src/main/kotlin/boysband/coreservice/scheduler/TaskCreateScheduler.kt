package boysband.coreservice.scheduler

import boysband.coreservice.service.TaskCreatorService
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component


@Component
class TaskCreateScheduler(
    private val handler: TaskCreatorService
) {
    private val exceptionHandler = CoroutineExceptionHandler { _, e ->
        logger.error("Scheduler coroutine failed", e)
    }
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob() + exceptionHandler)

    @Scheduled(fixedDelayString = $$"${scheduler.check-links}")
    fun run() {
        logger.info("start scheduler")
        scope.launch {
            handler.giveNewTasks()
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(TaskCreateScheduler::class.java)
    }
}