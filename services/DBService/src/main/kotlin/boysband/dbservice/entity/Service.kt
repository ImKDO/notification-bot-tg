package boysband.dbservice.entity

import jakarta.persistence.*

@Entity
@Table(name = "services")
class Service(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int = 0,

    @Column(nullable = false, length = 256)
    val link: String = "",

    @OneToMany(mappedBy = "service", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val methods: MutableList<Method> = mutableListOf(),

    @OneToMany(mappedBy = "service", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val filters: MutableList<Filter> = mutableListOf()
)
