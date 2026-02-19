package boysband.dbservice.service

import boysband.dbservice.entity.Action
import boysband.dbservice.entity.User
import boysband.dbservice.entity.Token
import boysband.dbservice.entity.Service as ServiceEntity
import boysband.dbservice.repository.ActionRepository
import boysband.dbservice.repository.UserRepository
import boysband.dbservice.repository.TokenRepository
import boysband.dbservice.repository.ServiceRepository
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.cache.annotation.Caching
import org.springframework.stereotype.Service
import java.util.Optional

@Service
class CachedDataService(
    private val actionRepository: ActionRepository,
    private val userRepository: UserRepository,
    private val tokenRepository: TokenRepository,
    private val serviceRepository: ServiceRepository,
) {

    // ── Users ────────────────────────────────────────────────────────────────

    @Cacheable(value = ["users"], key = "#id")
    fun findUserById(id: Int): Optional<User> = userRepository.findById(id)

    @Cacheable(value = ["userByTgChat"], key = "#idTgChat")
    fun findUserByTgChat(idTgChat: Long): User? = userRepository.findByIdTgChat(idTgChat)

    @Caching(evict = [
        CacheEvict(value = ["users"], key = "#result.id"),
        CacheEvict(value = ["userByTgChat"], key = "#result.idTgChat"),
    ])
    fun saveUser(user: User): User = userRepository.save(user)

    @Caching(evict = [
        CacheEvict(value = ["users"], key = "#id"),
        CacheEvict(value = ["userByTgChat"], allEntries = true),
    ])
    fun deleteUser(id: Int) = userRepository.deleteById(id)

    // ── Actions ──────────────────────────────────────────────────────────────
    // Actions involve deep lazy-loaded entity graphs (Method→Service, Token→User, etc.)
    // which don't serialize well to Redis. Direct DB access is used instead.

    fun findAllActions(): List<Action> = actionRepository.findAll()

    fun findActionById(id: Int): Optional<Action> = actionRepository.findById(id)

    fun findActionsByUserId(userId: Int): List<Action> = actionRepository.findAllByUserId(userId)

    fun findActionsByServiceId(serviceId: Int): List<Action> = actionRepository.findAllByServiceId(serviceId)

    fun findActionsByTelegramId(telegramId: Long): List<Action> = actionRepository.findAllByUserIdTgChat(telegramId)

    fun saveAction(action: Action): Action = actionRepository.save(action)

    fun existsAction(id: Int): Boolean = actionRepository.existsById(id)

    fun deleteAction(id: Int) = actionRepository.deleteById(id)

    // ── Tokens ───────────────────────────────────────────────────────────────

    @Cacheable(value = ["tokens"], key = "#id")
    fun findTokenById(id: Int): Optional<Token> = tokenRepository.findById(id)

    @Cacheable(value = ["tokensByUser"], key = "#userId")
    fun findTokensByUserId(userId: Int): List<Token> = tokenRepository.findAllByUserId(userId)

    @Caching(evict = [
        CacheEvict(value = ["tokens"], allEntries = true),
        CacheEvict(value = ["tokensByUser"], allEntries = true),
    ])
    fun saveToken(token: Token): Token = tokenRepository.save(token)

    @Caching(evict = [
        CacheEvict(value = ["tokens"], key = "#id"),
        CacheEvict(value = ["tokensByUser"], allEntries = true),
    ])
    fun deleteToken(id: Int) = tokenRepository.deleteById(id)

    fun existsToken(id: Int): Boolean = tokenRepository.existsById(id)

    // ── Services ─────────────────────────────────────────────────────────────

    @Cacheable(value = ["services"], key = "'all'")
    fun findAllServices(): List<ServiceEntity> = serviceRepository.findAll()

    @Cacheable(value = ["services"], key = "#id")
    fun findServiceById(id: Int): Optional<ServiceEntity> = serviceRepository.findById(id)

    @Cacheable(value = ["services"], key = "'name:' + #name.toLowerCase()")
    fun findServiceByName(name: String): ServiceEntity? = serviceRepository.findByNameIgnoreCase(name)
}
