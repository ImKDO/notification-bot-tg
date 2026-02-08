package boysband.dbservice.controller

import boysband.dbservice.entity.Action
import boysband.dbservice.repository.ActionRepository
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/actions")
class ActionController(private val actionRepository: ActionRepository) {

    @GetMapping
    fun getAllActions(): List<Action> = actionRepository.findAll()

    @GetMapping("/{id}")
    fun getActionById(@PathVariable id: Int): ResponseEntity<Action> {
        val action = actionRepository.findById(id)
        return if (action.isPresent) {
            ResponseEntity.ok(action.get())
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @GetMapping("/user/{userId}")
    fun getActionsByUserId(@PathVariable userId: Int): List<Action> {
        return actionRepository.findAllByUserId(userId)
    }

    @GetMapping("/service/{serviceId}")
    fun getActionsByServiceId(@PathVariable serviceId: Int): List<Action> {
        return actionRepository.findAllByServiceId(serviceId)
    }

    @PostMapping
    fun createAction(@RequestBody action: Action): ResponseEntity<Action> {
        val savedAction = actionRepository.save(action)
        return ResponseEntity.status(HttpStatus.CREATED).body(savedAction)
    }

    @PutMapping("/{id}")
    fun updateAction(@PathVariable id: Int, @RequestBody action: Action): ResponseEntity<Action> {
        return if (actionRepository.existsById(id)) {
            val savedAction = actionRepository.save(action)
            ResponseEntity.ok(savedAction)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @DeleteMapping("/{id}")
    fun deleteAction(@PathVariable id: Int): ResponseEntity<Void> {
        return if (actionRepository.existsById(id)) {
            actionRepository.deleteById(id)
            ResponseEntity.noContent().build()
        } else {
            ResponseEntity.notFound().build()
        }
    }
}
