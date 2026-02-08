package boysband.dbservice.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "actions")
class Action(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_method", nullable = false)
    val method: Method? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_token", nullable = false)
    val token: Token? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_tg_chat", referencedColumnName = "id_tg_chat", nullable = false)
    val user: User? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_service", nullable = false)
    val service: Service? = null,

    @Column(length = 16)
    val describe: String = "",

    @Column(length = 16384)
    val query: String = "",

    @Column(nullable = false)
    val date: LocalDateTime = LocalDateTime.now()
)
