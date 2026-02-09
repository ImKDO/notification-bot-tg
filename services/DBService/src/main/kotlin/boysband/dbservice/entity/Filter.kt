package boysband.dbservice.entity

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import jakarta.persistence.*

@Entity
@Table(name = "filters")
@JsonIgnoreProperties(value = ["hibernateLazyInitializer", "handler"])
class Filter(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_service", nullable = false)
    val service: Service? = null,

    @Column(nullable = false, length = 32)
    val name: String = "",

    @Column(nullable = false, length = 256)
    val describe: String = ""
)
