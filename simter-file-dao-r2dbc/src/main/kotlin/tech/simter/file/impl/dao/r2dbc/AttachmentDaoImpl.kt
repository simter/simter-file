package tech.simter.file.impl.dao.r2dbc

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.*
import org.springframework.data.domain.Sort.Direction.DESC
import org.springframework.data.r2dbc.core.DatabaseClient
import org.springframework.data.r2dbc.query.Criteria
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import tech.simter.file.TABLE_ATTACHMENT
import tech.simter.file.core.AttachmentDao
import tech.simter.file.core.domain.Attachment
import tech.simter.file.core.domain.AttachmentDto4Zip
import tech.simter.file.core.domain.AttachmentDtoWithChildren
import tech.simter.file.impl.dao.r2dbc.po.AttachmentPo
import java.util.*

/**
 * The spring-data-r2dbc implementation of [AttachmentDao].
 *
 * @author RJ
 */
@Repository
class AttachmentDaoImpl @Autowired constructor(
  private val databaseClient: DatabaseClient,
  private val repository: AttachmentRepository
) : AttachmentDao {
  @Suppress("UNCHECKED_CAST")
  override fun get(id: String): Mono<Attachment> {
    return repository.findById(id) as Mono<Attachment>
  }

  @Suppress("UNCHECKED_CAST")
  override fun find(pageable: Pageable): Mono<Page<Attachment>> {
    return Mono.zip(
      // query content
      databaseClient.select()
        .from(TABLE_ATTACHMENT)
        .page(PageRequest.of(
          pageable.pageNumber,
          if (pageable.pageSize < 1) 25 else pageable.pageSize,
          pageable.getSortOr(Sort.by(DESC, "create_on")) // default order by createOn desc
        ))
        .`as`(AttachmentPo::class.java)
        .fetch()
        .all()
        .collectList(),
      // query total count
      databaseClient.execute("select count(*) c from $TABLE_ATTACHMENT")
        .`as`(Long::class.javaObjectType)
        .fetch()
        .one()
    ) { content, total -> PageImpl(content, pageable, total) as Page<Attachment> }
      .defaultIfEmpty(Page.empty<Attachment>(pageable))
  }

  @Suppress("UNCHECKED_CAST")
  override fun find(puid: String, upperId: String?): Flux<Attachment> {
    val criteria = Criteria.where("puid").`is`(puid)
    return databaseClient.select()
      .from(TABLE_ATTACHMENT)
      .matching(if (null == upperId) criteria else criteria.and("upper_id").`is`(upperId))
      .orderBy(Sort.by(DESC, "create_on")) // default order by createOn desc
      .`as`(AttachmentPo::class.java)
      .fetch()
      .all() as Flux<Attachment>
  }

  override fun save(vararg attachments: Attachment): Mono<Void> {
    return if (attachments.isEmpty()) Mono.empty()
    else repository.saveAll(attachments.asIterable().map { AttachmentPo.from(it) }).then()
  }

  override fun delete(vararg ids: String): Flux<String> {
    TODO("not implemented")
  }

  override fun getFullPath(id: String): Mono<String> {
    val sql = """
      with recursive p(id, path, upper_id) as (
        select id, concat(path, ''), upper_id from st_attachment where id = :id
        union
        select a.id, concat(a.path, '/', p.path), a.upper_id from st_attachment as a
        join p on a.id = p.upper_id
      )
      select p.path from p where upper_id is null
    """.trimIndent()
    return databaseClient.execute(sql)
      .bind("id", id)
      .`as`(String::class.java)
      .fetch()
      .one()
  }

  override fun findDescendants(id: String): Flux<AttachmentDtoWithChildren> {
    TODO("not implemented")
  }

  override fun update(id: String, data: Map<String, Any?>): Mono<Void> {
    TODO("not implemented")
  }

  override fun findDescendantsZipPath(vararg ids: String): Flux<AttachmentDto4Zip> {
    TODO("not implemented")
  }

  override fun findPuids(vararg ids: String): Flux<Optional<String>> {
    TODO("not implemented")
  }
}