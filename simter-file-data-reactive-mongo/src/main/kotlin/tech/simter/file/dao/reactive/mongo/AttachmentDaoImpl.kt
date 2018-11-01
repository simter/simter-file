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
 * @author zh
 */
@Component
class AttachmentDaoImpl @Autowired constructor(
  @Value("\${simter.file.root}") private val fileRootDir: String,
  private val repository: AttachmentReactiveRepository,
  private val operations: ReactiveMongoOperations
) : AttachmentDao {
  override fun getFullPath(id: String): Mono<String> {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

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

  override fun find(puid: String, upperId: String?): Flux<Attachment> {
    val condition = Criteria.where("puid").`is`(puid)
    if (null != upperId) condition.and("upperId").`is`(upperId)
    return operations.find(Query.query(condition).with(Sort(Sort.Direction.DESC, "createOn")), Attachment::class.java)
  }

  override fun save(vararg attachments: Attachment): Mono<Void> {
    return if (attachments.isEmpty()) Mono.empty()
    else repository.saveAll(attachments.asIterable()).then()
  }

  override fun delete(vararg ids: String): Mono<Void> {
    return if (ids.isEmpty()) Mono.empty<Void>()
    else repository.findAllById(ids.asIterable()).collectList().flatMap {
      // delete attachment in database
      repository.deleteAll(it)
        // delete physics file
        .then(Mono.just(it.forEach { File("$fileRootDir/${it.path}").delete() }))
        .then(Mono.empty<Void>())
    }
  }
}