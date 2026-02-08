package boysband.dbservice.entity

import jakarta.persistence.*

@Entity
@Table(name = "methods")
class Method(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_service", nullable = false)
    val service: Service? = null,

    @Column(nullable = false, length = 16)
    val name: String = "",

    @Column(nullable = false, length = 256)
    val describe: String = "",

    @OneToMany(mappedBy = "method", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val actions: MutableList<Action> = mutableListOf()
)
