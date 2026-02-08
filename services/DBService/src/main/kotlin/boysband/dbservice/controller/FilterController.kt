package boysband.dbservice.controller

import boysband.dbservice.entity.Filter
import boysband.dbservice.repository.FilterRepository
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/filters")
class FilterController(private val filterRepository: FilterRepository) {

    @GetMapping
    fun getAllFilters(): List<Filter> = filterRepository.findAll()

    @GetMapping("/{id}")
    fun getFilterById(@PathVariable id: Int): ResponseEntity<Filter> {
        val filter = filterRepository.findById(id)
        return if (filter.isPresent) {
            ResponseEntity.ok(filter.get())
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @GetMapping("/service/{serviceId}")
    fun getFiltersByServiceId(@PathVariable serviceId: Int): List<Filter> {
        return filterRepository.findAllByServiceId(serviceId)
    }

    @PostMapping
    fun createFilter(@RequestBody filter: Filter): ResponseEntity<Filter> {
        val savedFilter = filterRepository.save(filter)
        return ResponseEntity.status(HttpStatus.CREATED).body(savedFilter)
    }

    @PutMapping("/{id}")
    fun updateFilter(@PathVariable id: Int, @RequestBody filter: Filter): ResponseEntity<Filter> {
        return if (filterRepository.existsById(id)) {
            val savedFilter = filterRepository.save(filter)
            ResponseEntity.ok(savedFilter)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @DeleteMapping("/{id}")
    fun deleteFilter(@PathVariable id: Int): ResponseEntity<Void> {
        return if (filterRepository.existsById(id)) {
            filterRepository.deleteById(id)
            ResponseEntity.noContent().build()
        } else {
            ResponseEntity.notFound().build()
        }
    }
}
