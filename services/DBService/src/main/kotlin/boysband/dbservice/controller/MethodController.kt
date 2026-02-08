package boysband.dbservice.controller

import boysband.dbservice.entity.Method
import boysband.dbservice.repository.MethodRepository
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/methods")
class MethodController(private val methodRepository: MethodRepository) {

    @GetMapping
    fun getAllMethods(): List<Method> = methodRepository.findAll()

    @GetMapping("/{id}")
    fun getMethodById(@PathVariable id: Int): ResponseEntity<Method> {
        val method = methodRepository.findById(id)
        return if (method.isPresent) {
            ResponseEntity.ok(method.get())
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @GetMapping("/service/{serviceId}")
    fun getMethodsByServiceId(@PathVariable serviceId: Int): List<Method> {
        return methodRepository.findAllByServiceId(serviceId)
    }

    @PostMapping
    fun createMethod(@RequestBody method: Method): ResponseEntity<Method> {
        val savedMethod = methodRepository.save(method)
        return ResponseEntity.status(HttpStatus.CREATED).body(savedMethod)
    }

    @PutMapping("/{id}")
    fun updateMethod(@PathVariable id: Int, @RequestBody method: Method): ResponseEntity<Method> {
        return if (methodRepository.existsById(id)) {
            val savedMethod = methodRepository.save(method)
            ResponseEntity.ok(savedMethod)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @DeleteMapping("/{id}")
    fun deleteMethod(@PathVariable id: Int): ResponseEntity<Void> {
        return if (methodRepository.existsById(id)) {
            methodRepository.deleteById(id)
            ResponseEntity.noContent().build()
        } else {
            ResponseEntity.notFound().build()
        }
    }
}
