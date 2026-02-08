package boysband.dbservice.controller

import boysband.dbservice.entity.HistoryAnswer
import boysband.dbservice.repository.HistoryAnswerRepository
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/history-answers")
class HistoryAnswerController(private val historyAnswerRepository: HistoryAnswerRepository) {

    @GetMapping
    fun getAllHistoryAnswers(): List<HistoryAnswer> = historyAnswerRepository.findAll()

    @GetMapping("/{id}")
    fun getHistoryAnswerById(@PathVariable id: Int): ResponseEntity<HistoryAnswer> {
        val historyAnswer = historyAnswerRepository.findById(id)
        return if (historyAnswer.isPresent) {
            ResponseEntity.ok(historyAnswer.get())
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @GetMapping("/user/{userId}")
    fun getHistoryAnswersByUserId(@PathVariable userId: Int): List<HistoryAnswer> {
        return historyAnswerRepository.findAllByUserId(userId)
    }

    @PostMapping
    fun createHistoryAnswer(@RequestBody historyAnswer: HistoryAnswer): ResponseEntity<HistoryAnswer> {
        val savedHistoryAnswer = historyAnswerRepository.save(historyAnswer)
        return ResponseEntity.status(HttpStatus.CREATED).body(savedHistoryAnswer)
    }

    @PutMapping("/{id}")
    fun updateHistoryAnswer(@PathVariable id: Int, @RequestBody historyAnswer: HistoryAnswer): ResponseEntity<HistoryAnswer> {
        return if (historyAnswerRepository.existsById(id)) {
            val savedHistoryAnswer = historyAnswerRepository.save(historyAnswer)
            ResponseEntity.ok(savedHistoryAnswer)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @DeleteMapping("/{id}")
    fun deleteHistoryAnswer(@PathVariable id: Int): ResponseEntity<Void> {
        return if (historyAnswerRepository.existsById(id)) {
            historyAnswerRepository.deleteById(id)
            ResponseEntity.noContent().build()
        } else {
            ResponseEntity.notFound().build()
        }
    }
}
