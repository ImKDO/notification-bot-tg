package boysband.dbservice.entity

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import jakarta.persistence.*

@Entity
@Table(name = "services")
@JsonIgnoreProperties(value = ["hibernateLazyInitializer", "handler"])
class Service(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int = 0,

    @Column(nullable = false, length = 256)
    val link: String = "",

    @Column(nullable = false, length = 256)
    val name: String = "",

    @OneToMany(mappedBy = "service", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    @JsonIgnore
    val methods: MutableList<Method> = mutableListOf(),

    @OneToMany(mappedBy = "service", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    @JsonIgnore
    val filters: MutableList<Filter> = mutableListOf()
)
