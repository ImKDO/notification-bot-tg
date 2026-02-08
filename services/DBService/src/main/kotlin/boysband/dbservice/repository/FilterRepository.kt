package boysband.dbservice.repository

import boysband.dbservice.entity.Filter
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface FilterRepository : JpaRepository<Filter, Int> {
    fun findAllByServiceId(serviceId: Int): List<Filter>
}
