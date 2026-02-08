package boysband.dbservice.repository

import boysband.dbservice.entity.RequestsNewService
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface RequestsNewServiceRepository : JpaRepository<RequestsNewService, Int> {
    fun findAllByUserId(userId: Int): List<RequestsNewService>
}
