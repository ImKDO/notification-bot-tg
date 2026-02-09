package boysband.dbservice.repository

import boysband.dbservice.entity.Action
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ActionRepository : JpaRepository<Action, Int> {
    fun findAllByUserId(userId: Int): List<Action>
    fun findAllByServiceId(serviceId: Int): List<Action>
    fun findAllByUserIdTgChat(idTgChat: Long): List<Action>
}
