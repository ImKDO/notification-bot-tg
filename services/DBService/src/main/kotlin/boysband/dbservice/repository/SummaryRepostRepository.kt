package boysband.dbservice.repository

import boysband.dbservice.entity.SummaryRepost
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface SummaryRepostRepository : JpaRepository<SummaryRepost, Int> {
    fun findAllByUserId(userId: Int): List<SummaryRepost>
    fun findAllByUserIdTgChat(idTgChat: Long): List<SummaryRepost>
    fun findFirstByUserIdTgChatOrderByDateDesc(idTgChat: Long): SummaryRepost?
}
