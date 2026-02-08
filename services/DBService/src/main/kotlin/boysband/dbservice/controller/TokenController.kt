package boysband.dbservice.controller

import boysband.dbservice.entity.Token
import boysband.dbservice.repository.TokenRepository
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/tokens")
class TokenController(private val tokenRepository: TokenRepository) {

    @GetMapping
    fun getAllTokens(): List<Token> = tokenRepository.findAll()

    @GetMapping("/{id}")
    fun getTokenById(@PathVariable id: Int): ResponseEntity<Token> {
        val token = tokenRepository.findById(id)
        return if (token.isPresent) {
            ResponseEntity.ok(token.get())
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @GetMapping("/user/{userId}")
    fun getTokensByUserId(@PathVariable userId: Int): List<Token> {
        return tokenRepository.findAllByUserId(userId)
    }

    @PostMapping
    fun createToken(@RequestBody token: Token): ResponseEntity<Token> {
        val savedToken = tokenRepository.save(token)
        return ResponseEntity.status(HttpStatus.CREATED).body(savedToken)
    }

    @PutMapping("/{id}")
    fun updateToken(@PathVariable id: Int, @RequestBody token: Token): ResponseEntity<Token> {
        return if (tokenRepository.existsById(id)) {
            val savedToken = tokenRepository.save(token)
            ResponseEntity.ok(savedToken)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @DeleteMapping("/{id}")
    fun deleteToken(@PathVariable id: Int): ResponseEntity<Void> {
        return if (tokenRepository.existsById(id)) {
            tokenRepository.deleteById(id)
            ResponseEntity.noContent().build()
        } else {
            ResponseEntity.notFound().build()
        }
    }
}
