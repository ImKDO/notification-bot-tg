package boysband.dbservice.controller

import boysband.dbservice.entity.Service
import boysband.dbservice.repository.ServiceRepository
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/services")
class ServiceController(private val serviceRepository: ServiceRepository) {

    @GetMapping
    fun getAllServices(): List<Service> = serviceRepository.findAll()

    @GetMapping("/{id}")
    fun getServiceById(@PathVariable id: Int): ResponseEntity<Service> {
        val service = serviceRepository.findById(id)
        return if (service.isPresent) {
            ResponseEntity.ok(service.get())
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @PostMapping
    fun createService(@RequestBody service: Service): ResponseEntity<Service> {
        val savedService = serviceRepository.save(service)
        return ResponseEntity.status(HttpStatus.CREATED).body(savedService)
    }

    @PutMapping("/{id}")
    fun updateService(@PathVariable id: Int, @RequestBody service: Service): ResponseEntity<Service> {
        return if (serviceRepository.existsById(id)) {
            val savedService = serviceRepository.save(service)
            ResponseEntity.ok(savedService)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @DeleteMapping("/{id}")
    fun deleteService(@PathVariable id: Int): ResponseEntity<Void> {
        return if (serviceRepository.existsById(id)) {
            serviceRepository.deleteById(id)
            ResponseEntity.noContent().build()
        } else {
            ResponseEntity.notFound().build()
        }
    }
}
