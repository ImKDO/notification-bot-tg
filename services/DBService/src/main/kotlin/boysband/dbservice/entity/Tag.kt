package boysband.dbservice.entity

import jakarta.persistence.*

@Entity
@Table(name = "tags")
class Tag(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_tg_chat", referencedColumnName = "id_tg_chat", nullable = false)
    val user: User? = null,

    @Column(nullable = false, length = 64)
    val name: String = ""
)
