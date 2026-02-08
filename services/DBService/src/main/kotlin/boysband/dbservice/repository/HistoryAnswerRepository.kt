package boysband.dbservice.repository

import boysband.dbservice.entity.HistoryAnswer
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface HistoryAnswerRepository : JpaRepository<HistoryAnswer, Int> {
    fun findAllByUserId(userId: Int): List<HistoryAnswer>
}
