package boysband.dbservice.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "requests_new_services")
class RequestsNewService(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_tg_chat", referencedColumnName = "id_tg_chat", nullable = false)
    val user: User? = null,

    @Column(nullable = false, length = 256)
    val link: String = "",

    @Column(nullable = false)
    val date: LocalDateTime = LocalDateTime.now()
)
