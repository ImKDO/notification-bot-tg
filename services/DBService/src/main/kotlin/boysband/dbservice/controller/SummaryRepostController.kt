package boysband.dbservice.controller

import boysband.dbservice.entity.SummaryRepost
import boysband.dbservice.repository.SummaryRepostRepository
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/summary-reposts")
class SummaryRepostController(private val summaryRepostRepository: SummaryRepostRepository) {

    @GetMapping
    fun getAllSummaryReposts(): List<SummaryRepost> = summaryRepostRepository.findAll()

    @GetMapping("/{id}")
    fun getSummaryRepostById(@PathVariable id: Int): ResponseEntity<SummaryRepost> {
        val summaryRepost = summaryRepostRepository.findById(id)
        return if (summaryRepost.isPresent) {
            ResponseEntity.ok(summaryRepost.get())
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @GetMapping("/user/{userId}")
    fun getSummaryRepostsByUserId(@PathVariable userId: Int): List<SummaryRepost> {
        return summaryRepostRepository.findAllByUserId(userId)
    }

    @PostMapping
    fun createSummaryRepost(@RequestBody summaryRepost: SummaryRepost): ResponseEntity<SummaryRepost> {
        val savedSummaryRepost = summaryRepostRepository.save(summaryRepost)
        return ResponseEntity.status(HttpStatus.CREATED).body(savedSummaryRepost)
    }

    @PutMapping("/{id}")
    fun updateSummaryRepost(@PathVariable id: Int, @RequestBody summaryRepost: SummaryRepost): ResponseEntity<SummaryRepost> {
        return if (summaryRepostRepository.existsById(id)) {
            val savedSummaryRepost = summaryRepostRepository.save(summaryRepost)
            ResponseEntity.ok(savedSummaryRepost)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @DeleteMapping("/{id}")
    fun deleteSummaryRepost(@PathVariable id: Int): ResponseEntity<Void> {
        return if (summaryRepostRepository.existsById(id)) {
            summaryRepostRepository.deleteById(id)
            ResponseEntity.noContent().build()
        } else {
            ResponseEntity.notFound().build()
        }
    }
}
