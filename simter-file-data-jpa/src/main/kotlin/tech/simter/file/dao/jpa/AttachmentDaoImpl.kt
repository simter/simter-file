package tech.simter.file.dao.jpa

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import tech.simter.file.dao.AttachmentDao
import tech.simter.file.dto.AttachmentDtoWithChildren
import tech.simter.file.po.Attachment
import java.io.File
import javax.persistence.EntityManager
import javax.persistence.PersistenceContext
import javax.persistence.Query

/**
 * The JPA implementation of [AttachmentDao].
 *
 * @author RJ
 * @author zh
 */
@Component
class AttachmentDaoImpl @Autowired constructor(
  @Value("\${simter.file.root}") private val fileRootDir: String,
  @PersistenceContext private val em: EntityManager,
  private val repository: AttachmentJpaRepository
) : AttachmentDao {
  override fun update(id: String, data: Map<String, Any?>): Mono<Void> {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun findDescendents(id: String): Flux<AttachmentDtoWithChildren> {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun getFullPath(id: String): Mono<String> {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun get(id: String): Mono<Attachment> {
    return Mono.justOrEmpty(repository.findById(id))
  }

  override fun find(pageable: Pageable): Mono<Page<Attachment>> {
    return Mono.justOrEmpty(repository.findAll(pageable))
  }

  @Suppress("UNCHECKED_CAST")
  override fun find(puid: String, upperId: String?): Flux<Attachment> {
    val hasSubgroup = null != upperId
    val sql = """
      select a from Attachment a
      where puid = :puid ${if (hasSubgroup) "and upperId = :upperId" else ""}
      order by createOn desc
    """.trimIndent()
    val query: Query = em.createQuery(sql, Attachment::class.java).setParameter("puid", puid)
    if (hasSubgroup) query.setParameter("upperId", upperId)
    return Flux.fromIterable(query.resultList as List<Attachment>)
  }

  override fun save(vararg attachments: Attachment): Mono<Void> {
    repository.saveAll(attachments.asIterable())
    return Mono.empty()
  }

  override fun delete(vararg ids: String): Mono<Void> {
    if (!ids.isEmpty()) {
      val attachments = repository.findAllById(ids.toList())
      if (!attachments.isEmpty()) {
        repository.deleteAll(attachments)
        attachments.forEach { File("$fileRootDir/${it.path}").delete() }
      }
    }
    return Mono.empty<Void>()
  }
}
