package boysband.dbservice.entity

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import jakarta.persistence.*

@Entity
@Table(name = "methods")
@JsonIgnoreProperties(value = ["hibernateLazyInitializer", "handler"])
class Method(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int = 0,

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_service", nullable = false)
    val service: Service? = null,

    @Column(nullable = false, length = 16)
    val name: String = "",

    @Column(nullable = false, length = 256)
    val describe: String = "",

    @OneToMany(mappedBy = "method", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    @JsonIgnore
    val actions: MutableList<Action> = mutableListOf()
)
