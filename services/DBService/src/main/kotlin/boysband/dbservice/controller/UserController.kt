package boysband.dbservice.controller

import boysband.dbservice.entity.User
import boysband.dbservice.repository.UserRepository
import boysband.dbservice.service.CachedDataService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/users")
class UserController(
    private val userRepository: UserRepository,
    private val cachedDataService: CachedDataService,
) {

    @GetMapping
    fun getAllUsers(): List<User> = userRepository.findAll()

    @GetMapping("/{id}")
    fun getUserById(@PathVariable id: Int): ResponseEntity<User> {
        val user = cachedDataService.findUserById(id)
        return if (user.isPresent) {
            ResponseEntity.ok(user.get())
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @GetMapping("/tg-chat/{idTgChat}")
    fun getUserByTgChat(@PathVariable idTgChat: Long): ResponseEntity<User> {
        val user = cachedDataService.findUserByTgChat(idTgChat)
        return if (user != null) {
            ResponseEntity.ok(user)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @PostMapping
    fun createUser(@RequestBody user: User): ResponseEntity<User> {
        val existing = cachedDataService.findUserByTgChat(user.idTgChat)
        if (existing != null) {
            return ResponseEntity.ok(existing)
        }
        val savedUser = cachedDataService.saveUser(user)
        return ResponseEntity.status(HttpStatus.CREATED).body(savedUser)
    }

    @PutMapping("/{id}")
    fun updateUser(@PathVariable id: Int, @RequestBody user: User): ResponseEntity<User> {
        return if (userRepository.existsById(id)) {
            val savedUser = cachedDataService.saveUser(user)
            ResponseEntity.ok(savedUser)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @DeleteMapping("/{id}")
    fun deleteUser(@PathVariable id: Int): ResponseEntity<Void> {
        return if (userRepository.existsById(id)) {
            cachedDataService.deleteUser(id)
            ResponseEntity.noContent().build()
        } else {
            ResponseEntity.notFound().build()
        }
    }
}
