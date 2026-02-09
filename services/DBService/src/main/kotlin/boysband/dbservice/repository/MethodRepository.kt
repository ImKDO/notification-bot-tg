package boysband.dbservice.repository

import boysband.dbservice.entity.Method
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface MethodRepository : JpaRepository<Method, Int> {
    fun findAllByServiceId(serviceId: Int): List<Method>
    fun findByNameIgnoreCase(name: String): Method?
}
