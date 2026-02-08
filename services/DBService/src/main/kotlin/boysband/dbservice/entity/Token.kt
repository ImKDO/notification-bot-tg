package boysband.dbservice.entity

import jakarta.persistence.*

@Entity
@Table(name = "tokens")
class Token(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int = 0,

    @Column(nullable = false, length = 256)
    val value: String = "",

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_tg_chat", referencedColumnName = "id_tg_chat", nullable = false)
    val user: User? = null
)
