package tech.simter.file.dao.reactive.mongo

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import tech.simter.file.dao.AttachmentDao
import tech.simter.file.po.Attachment
import java.io.File

/**
 * The Reactive MongoDB implementation of [AttachmentDao].
 *
 * @author RJ
 */
@Component
class AttachmentDaoImpl @Autowired constructor(
  @Value("\${simter.file.root}") private val fileRootDir: String,
  private val repository: AttachmentReactiveRepository,
  private val operations: ReactiveMongoOperations
) : AttachmentDao {
  override fun get(id: String): Mono<Attachment> {
    return repository.findById(id)
  }

  override fun find(pageable: Pageable): Mono<Page<Attachment>> {
    val query = Query().with(pageable)
    val zip: Mono<Page<Attachment>> = Mono.zip(
      operations.find(query, Attachment::class.java).collectList(),
      operations.count(query, Attachment::class.java),
      { content, total -> PageImpl(content, pageable, total) }
    )
    return zip.defaultIfEmpty(Page.empty<Attachment>(pageable))
  }

  override fun find(puid: String, subgroup: Short?): Flux<Attachment> {
    val condition = Criteria.where("puid").`is`(puid)
    if (null != subgroup) condition.and("subgroup").`is`(subgroup)
    return operations.find(Query.query(condition).with(Sort(Sort.Direction.DESC, "uploadOn")), Attachment::class.java)
  }

  override fun find(vararg ids: String): Flux<Attachment> {
    return ids.let {
      if (it.isEmpty()) throw NullPointerException("The ids parameter must not be empty")
      else repository.findAllById(ids.asIterable())
    }
  }

  override fun save(vararg attachments: Attachment): Mono<Void> {
    return if (attachments.isEmpty()) Mono.empty()
    else repository.saveAll(attachments.asIterable()).then()
  }

  override fun delete(vararg ids: String): Mono<Void> {
    return if (ids.isEmpty()) Mono.empty()
    else repository.findAllById(ids.asIterable()).collectList().flatMap {
      if (it.isEmpty()) Mono.empty<Void>()
      // delete physics file
      it.forEach { File("$fileRootDir/${it.path}").delete() }
      // delete attachment in database
      repository.deleteAll(it.asIterable())
    }
  }
}