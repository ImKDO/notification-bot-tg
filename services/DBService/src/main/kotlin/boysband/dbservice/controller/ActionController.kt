package boysband.dbservice.controller

import boysband.dbservice.entity.Action
import boysband.dbservice.entity.User
import boysband.dbservice.kafka.SubscriptionProducer
import boysband.dbservice.repository.ActionRepository
import boysband.dbservice.repository.MethodRepository
import boysband.dbservice.repository.ServiceRepository
import boysband.dbservice.repository.TokenRepository
import boysband.dbservice.repository.UserRepository
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/actions")
class ActionController(
    private val actionRepository: ActionRepository,
    private val userRepository: UserRepository,
    private val tokenRepository: TokenRepository,
    private val methodRepository: MethodRepository,
    private val serviceRepository: ServiceRepository,
    private val subscriptionProducer: SubscriptionProducer,
) {

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

    // ── Subscribe endpoint (auto-resolves service/method/token by name) ─────

    data class SubscribeRequest(
        val telegramId: Long = 0,
        val methodName: String = "",
        val query: String = "",
        val serviceName: String = "GitHub",
        val describe: String = "",
    )

    @PostMapping("/subscribe")
    fun subscribe(@RequestBody request: SubscribeRequest): ResponseEntity<Any> {
        // Find user
        val user = userRepository.findByIdTgChat(request.telegramId)
            ?: return ResponseEntity.badRequest()
                .body(mapOf("error" to "Пользователь не найден. Сначала выполните /start"))

        // Find service by name
        val service = serviceRepository.findByNameIgnoreCase(request.serviceName)
            ?: return ResponseEntity.badRequest()
                .body(mapOf("error" to "Сервис '${request.serviceName}' не найден"))

        // Find method by name
        val method = methodRepository.findByNameIgnoreCase(request.methodName)
            ?: return ResponseEntity.badRequest()
                .body(mapOf("error" to "Метод '${request.methodName}' не найден"))

        // Find user's token (need at least one for GitHub API calls)
        val tokens = tokenRepository.findAllByUserId(user.id)
        if (tokens.isEmpty()) {
            return ResponseEntity.badRequest()
                .body(mapOf("error" to "Токен не найден. Сначала авторизуйте сервис"))
        }
        val token = tokens.first()

        // Create action
        val savedAction = actionRepository.save(
            Action(
                method = method,
                token = token,
                user = user,
                service = service,
                query = request.query,
                describe = request.describe,
            )
        )

        // Send to Kafka for immediate processing by CoreService
        subscriptionProducer.sendSubscriptionRequest(savedAction)

        return ResponseEntity.status(HttpStatus.CREATED).body(savedAction)
    }

    @PostMapping
    fun createAction(@RequestBody action: Action): ResponseEntity<Action> {
        val requestUser = action.user
            ?: return ResponseEntity.badRequest().build()

        val persistedUser = when {
            requestUser.idTgChat != 0L -> userRepository.findByIdTgChat(requestUser.idTgChat)
                ?: userRepository.save(User(idTgChat = requestUser.idTgChat))
            requestUser.id != 0 -> userRepository.findById(requestUser.id).orElse(null)
            else -> null
        } ?: return ResponseEntity.badRequest().build()

        val requestTokenId = action.token?.id ?: 0
        val requestMethodId = action.method?.id ?: 0
        val requestServiceId = action.service?.id ?: 0
        if (requestTokenId == 0 || requestMethodId == 0 || requestServiceId == 0) {
            return ResponseEntity.badRequest().build()
        }

        val persistedToken = tokenRepository.findById(requestTokenId).orElse(null)
            ?: return ResponseEntity.badRequest().build()
        val persistedMethod = methodRepository.findById(requestMethodId).orElse(null)
            ?: return ResponseEntity.badRequest().build()
        val persistedService = serviceRepository.findById(requestServiceId).orElse(null)
            ?: return ResponseEntity.badRequest().build()

        val savedAction = actionRepository.save(
            Action(
                id = action.id,
                method = persistedMethod,
                token = persistedToken,
                user = persistedUser,
                service = persistedService,
                describe = action.describe,
                query = action.query,
                date = action.date,
                lastCheckDate = action.lastCheckDate,
            ),
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(savedAction)
    }

    @PutMapping("/{id}")
    fun updateAction(@PathVariable id: Int, @RequestBody action: Action): ResponseEntity<Action> {
        return if (actionRepository.existsById(id)) {
            val requestUser = action.user
                ?: return ResponseEntity.badRequest().build()

            val persistedUser = when {
                requestUser.idTgChat != 0L -> userRepository.findByIdTgChat(requestUser.idTgChat)
                    ?: userRepository.save(User(idTgChat = requestUser.idTgChat))
                requestUser.id != 0 -> userRepository.findById(requestUser.id).orElse(null)
                else -> null
            } ?: return ResponseEntity.badRequest().build()

            val requestTokenId = action.token?.id ?: 0
            val requestMethodId = action.method?.id ?: 0
            val requestServiceId = action.service?.id ?: 0
            if (requestTokenId == 0 || requestMethodId == 0 || requestServiceId == 0) {
                return ResponseEntity.badRequest().build()
            }

            val persistedToken = tokenRepository.findById(requestTokenId).orElse(null)
                ?: return ResponseEntity.badRequest().build()
            val persistedMethod = methodRepository.findById(requestMethodId).orElse(null)
                ?: return ResponseEntity.badRequest().build()
            val persistedService = serviceRepository.findById(requestServiceId).orElse(null)
                ?: return ResponseEntity.badRequest().build()

            val savedAction = actionRepository.save(
                Action(
                    id = id,
                    method = persistedMethod,
                    token = persistedToken,
                    user = persistedUser,
                    service = persistedService,
                    describe = action.describe,
                    query = action.query,
                    date = action.date,
                    lastCheckDate = action.lastCheckDate,
                ),
            )
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
