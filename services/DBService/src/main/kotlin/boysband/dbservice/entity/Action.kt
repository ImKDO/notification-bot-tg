package boysband.dbservice.entity

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import jakarta.persistence.*
import java.time.LocalDateTime
import java.time.ZonedDateTime

@Entity
@Table(name = "actions")
@JsonIgnoreProperties(value = ["hibernateLazyInitializer", "handler"])
class Action(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_method", nullable = false)
    val method: Method? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_token", nullable = true)
    val token: Token? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_tg_chat", referencedColumnName = "id_tg_chat", nullable = false)
    @JsonIgnoreProperties(
        value = [
            "tokens",
            "actions",
            "tags",
            "historyAnswers",
            "summaryReposts",
            "requestsNewServices",
            "hibernateLazyInitializer",
            "handler",
        ],
        allowSetters = true,
    )
    val user: User? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_service", nullable = false)
    val service: Service? = null,

    @Column(length = 64)
    val describe: String = "",

    @Column(length = 16384)
    val query: String = "",

    @Column(nullable = false)
    val date: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    val lastCheckDate: ZonedDateTime = ZonedDateTime.now()
)
