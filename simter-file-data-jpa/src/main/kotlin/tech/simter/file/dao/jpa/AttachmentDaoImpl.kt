package tech.simter.file.dao.jpa

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import tech.simter.file.dao.AttachmentDao
import tech.simter.file.po.Attachment
import javax.persistence.EntityManager
import javax.persistence.PersistenceContext
import javax.persistence.Query

/**
 * The JPA implementation of [AttachmentDao].
 *
 * @author RJ
 */
@Component
class AttachmentDaoImpl @Autowired constructor(
  @PersistenceContext private val em: EntityManager,
  private val repository: AttachmentJpaRepository
) : AttachmentDao {
  override fun get(id: String): Mono<Attachment> {
    return Mono.justOrEmpty(repository.findById(id))
  }

  override fun find(pageable: Pageable): Mono<Page<Attachment>> {
    return Mono.justOrEmpty(repository.findAll(pageable))
  }

  @Suppress("UNCHECKED_CAST")
  override fun find(puid: String, subgroup: Short?): Flux<Attachment> {
    val hasSubgroup = null != subgroup
    val sql = """
      select a from Attachment a
      where puid = :puid ${if (hasSubgroup) "and subgroup = :subgroup" else ""}
      order by uploadOn desc
    """.trimIndent()
    val query: Query = em.createQuery(sql, Attachment::class.java).setParameter("puid", puid)
    if (hasSubgroup) query.setParameter("subgroup", subgroup)
    return Flux.fromIterable(query.resultList as List<Attachment>)
  }

  override fun save(vararg attachments: Attachment): Mono<Void> {
    repository.saveAll(attachments.asIterable())
    return Mono.empty()
  }

  override fun delete(vararg ids: String): Mono<Void> {
    if (!ids.isEmpty()) {
      em.createQuery("delete from Attachment where id in (:ids)")
        .setParameter("ids", ids.toList())
        .executeUpdate()
    }

    return Mono.empty()
  }
}