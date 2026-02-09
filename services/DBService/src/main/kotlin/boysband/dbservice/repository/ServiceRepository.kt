package boysband.dbservice.repository

import boysband.dbservice.entity.Service
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ServiceRepository : JpaRepository<Service, Int> {
    fun findByNameIgnoreCase(name: String): Service?
}
