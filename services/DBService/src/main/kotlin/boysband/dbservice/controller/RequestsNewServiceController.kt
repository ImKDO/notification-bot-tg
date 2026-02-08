package boysband.dbservice.controller

import boysband.dbservice.entity.RequestsNewService
import boysband.dbservice.repository.RequestsNewServiceRepository
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/requests-new-services")
class RequestsNewServiceController(private val requestsNewServiceRepository: RequestsNewServiceRepository) {

    @GetMapping
    fun getAllRequestsNewServices(): List<RequestsNewService> = requestsNewServiceRepository.findAll()

    @GetMapping("/{id}")
    fun getRequestsNewServiceById(@PathVariable id: Int): ResponseEntity<RequestsNewService> {
        val requestsNewService = requestsNewServiceRepository.findById(id)
        return if (requestsNewService.isPresent) {
            ResponseEntity.ok(requestsNewService.get())
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @GetMapping("/user/{userId}")
    fun getRequestsNewServicesByUserId(@PathVariable userId: Int): List<RequestsNewService> {
        return requestsNewServiceRepository.findAllByUserId(userId)
    }

    @PostMapping
    fun createRequestsNewService(@RequestBody requestsNewService: RequestsNewService): ResponseEntity<RequestsNewService> {
        val savedRequestsNewService = requestsNewServiceRepository.save(requestsNewService)
        return ResponseEntity.status(HttpStatus.CREATED).body(savedRequestsNewService)
    }

    @PutMapping("/{id}")
    fun updateRequestsNewService(@PathVariable id: Int, @RequestBody requestsNewService: RequestsNewService): ResponseEntity<RequestsNewService> {
        return if (requestsNewServiceRepository.existsById(id)) {
            val savedRequestsNewService = requestsNewServiceRepository.save(requestsNewService)
            ResponseEntity.ok(savedRequestsNewService)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @DeleteMapping("/{id}")
    fun deleteRequestsNewService(@PathVariable id: Int): ResponseEntity<Void> {
        return if (requestsNewServiceRepository.existsById(id)) {
            requestsNewServiceRepository.deleteById(id)
            ResponseEntity.noContent().build()
        } else {
            ResponseEntity.notFound().build()
        }
    }
}
