package boysband.dbservice.repository

import boysband.dbservice.entity.Token
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface TokenRepository : JpaRepository<Token, Int> {
    fun findAllByUserId(userId: Int): List<Token>
}
