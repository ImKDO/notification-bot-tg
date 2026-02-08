package boysband.dbservice.repository

import boysband.dbservice.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface UserRepository : JpaRepository<User, Int> {
    fun findByIdTgChat(idTgChat: Int): User?
}
