package boysband.dbservice.entity

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import jakarta.persistence.*

@Entity
@Table(name = "tokens")
@JsonIgnoreProperties(value = ["hibernateLazyInitializer", "handler"])
class Token(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int = 0,

    @Column(nullable = false, length = 256)
    val value: String = "",

    @ManyToOne(fetch = FetchType.EAGER)
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
    val user: User? = null
)
