package boysband.dbservice.controller

import boysband.dbservice.entity.Token
import boysband.dbservice.entity.User
import boysband.dbservice.kafka.TokenValidationProducer
import boysband.dbservice.kafka.TokenValidationRequestDto
import boysband.dbservice.repository.TokenRepository
import boysband.dbservice.repository.UserRepository
import boysband.dbservice.service.CachedDataService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/tokens")
class TokenController(
    private val tokenRepository: TokenRepository,
    private val userRepository: UserRepository,
    private val tokenValidationProducer: TokenValidationProducer,
    private val cachedDataService: CachedDataService,
) {

    @GetMapping
    fun getAllTokens(): List<Token> = tokenRepository.findAll()

    @GetMapping("/{id}")
    fun getTokenById(@PathVariable id: Int): ResponseEntity<Token> {
        val token = cachedDataService.findTokenById(id)
        return if (token.isPresent) {
            ResponseEntity.ok(token.get())
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @GetMapping("/user/{userId}")
    fun getTokensByUserId(@PathVariable userId: Int): List<Token> {
        return cachedDataService.findTokensByUserId(userId)
    }

    @PostMapping
    fun createToken(@RequestBody token: Token): ResponseEntity<Token> {
        val requestUser = token.user
            ?: return ResponseEntity.badRequest().build()

        val persistedUser = cachedDataService.findUserByTgChat(requestUser.idTgChat)
            ?: try {
                cachedDataService.saveUser(User(idTgChat = requestUser.idTgChat))
            } catch (_: Exception) {
                cachedDataService.findUserByTgChat(requestUser.idTgChat)
                    ?: return ResponseEntity.badRequest().build()
            }

        val savedToken = cachedDataService.saveToken(Token(value = token.value, user = persistedUser))
        return ResponseEntity.status(HttpStatus.CREATED).body(savedToken)
    }

    @PutMapping("/{id}")
    fun updateToken(@PathVariable id: Int, @RequestBody token: Token): ResponseEntity<Token> {
        return if (cachedDataService.existsToken(id)) {
            val requestUser = token.user
                ?: return ResponseEntity.badRequest().build()

            val persistedUser = cachedDataService.findUserByTgChat(requestUser.idTgChat)
                ?: cachedDataService.saveUser(User(idTgChat = requestUser.idTgChat))

            val savedToken = cachedDataService.saveToken(Token(id = id, value = token.value, user = persistedUser))
            ResponseEntity.ok(savedToken)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @DeleteMapping("/{id}")
    fun deleteToken(@PathVariable id: Int): ResponseEntity<Void> {
        return if (cachedDataService.existsToken(id)) {
            cachedDataService.deleteToken(id)
            ResponseEntity.noContent().build()
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @PostMapping("/validate")
    fun validateToken(@RequestBody request: TokenValidationRequestDto): ResponseEntity<Map<String, String>> {
        tokenValidationProducer.sendTokenValidationRequest(request)
        return ResponseEntity.ok(mapOf("status" to "sent_for_validation"))
    }
}
