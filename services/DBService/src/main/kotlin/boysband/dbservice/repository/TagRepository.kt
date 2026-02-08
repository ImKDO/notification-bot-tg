package boysband.dbservice.repository

import boysband.dbservice.entity.Tag
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface TagRepository : JpaRepository<Tag, Int> {
    fun findAllByUserId(userId: Int): List<Tag>
}
