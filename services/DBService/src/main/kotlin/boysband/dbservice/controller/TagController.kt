package boysband.dbservice.controller

import boysband.dbservice.entity.Tag
import boysband.dbservice.repository.TagRepository
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/tags")
class TagController(private val tagRepository: TagRepository) {

    @GetMapping
    fun getAllTags(): List<Tag> = tagRepository.findAll()

    @GetMapping("/{id}")
    fun getTagById(@PathVariable id: Int): ResponseEntity<Tag> {
        val tag = tagRepository.findById(id)
        return if (tag.isPresent) {
            ResponseEntity.ok(tag.get())
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @GetMapping("/user/{userId}")
    fun getTagsByUserId(@PathVariable userId: Int): List<Tag> {
        return tagRepository.findAllByUserId(userId)
    }

    @PostMapping
    fun createTag(@RequestBody tag: Tag): ResponseEntity<Tag> {
        val savedTag = tagRepository.save(tag)
        return ResponseEntity.status(HttpStatus.CREATED).body(savedTag)
    }

    @PutMapping("/{id}")
    fun updateTag(@PathVariable id: Int, @RequestBody tag: Tag): ResponseEntity<Tag> {
        return if (tagRepository.existsById(id)) {
            val savedTag = tagRepository.save(tag)
            ResponseEntity.ok(savedTag)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @DeleteMapping("/{id}")
    fun deleteTag(@PathVariable id: Int): ResponseEntity<Void> {
        return if (tagRepository.existsById(id)) {
            tagRepository.deleteById(id)
            ResponseEntity.noContent().build()
        } else {
            ResponseEntity.notFound().build()
        }
    }
}
