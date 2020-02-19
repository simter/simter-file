package tech.simter.file.impl.dao.mongo

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import org.springframework.data.mongodb.core.aggregation.Aggregation.*
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.data.mongodb.core.remove
import org.springframework.data.mongodb.core.updateMulti
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import tech.simter.exception.NotFoundException
import tech.simter.file.core.AttachmentDao
import tech.simter.file.core.domain.Attachment
import tech.simter.file.core.domain.AttachmentDto4Zip
import tech.simter.file.core.domain.AttachmentDtoWithChildren
import tech.simter.file.impl.dao.mongo.dto.*
import tech.simter.file.impl.dao.mongo.po.AttachmentPo
import java.util.*

/**
 * The Reactive MongoDB implementation of [AttachmentDao].
 *
 * @author RJ
 * @author zh
 */
@Repository
class AttachmentDaoImpl @Autowired constructor(
  private val repository: AttachmentReactiveRepository,
  private val operations: ReactiveMongoOperations
) : AttachmentDao {
  override fun findPuids(vararg ids: String): Flux<Optional<String>> {
    val query = Query.query(Criteria.where("_id").`in`(*ids))
      .also { it.fields().include("puid").exclude("_id") }
    return operations.find(query, AttachmentPuid::class.java, operations.getCollectionName(AttachmentPo::class.java))
      .map { Optional.ofNullable(it.puid) }.distinct()
  }

  override fun findDescendantsZipPath(vararg ids: String): Flux<AttachmentDto4Zip> {
    return operations.aggregate(
      newAggregation(
        // Filter out the specified Attachment
        match(Criteria.where("id").`in`(*ids)),
        // Aggregate all upper(including itself) into an array into field "aggregate"
        graphLookup("st_attachment")
          .startWith("id").connectFrom("upperId").connectTo("_id").`as`("uppers"),
        // Aggregate all descendants into an array into field "aggregate"
        graphLookup("st_attachment")
          .startWith("id").connectFrom("_id").connectTo("upperId").`as`("descendants"),
        project("uppers", "descendants", "type")
      ),
      AttachmentPo::class.java,
      AttachmentUppersWithDescendants::class.java
    )
      .collectList().flatMapIterable { it.convertToAttachmentDto4Zip() }
  }

  @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
  override fun update(id: String, data: Map<String, Any?>): Mono<Void> {
    return if (data.isEmpty()) Mono.empty()
    else operations.updateMulti<AttachmentPo>(
      // Filter out the specified Attachment
      Query.query(Criteria.where("id").`is`(id)),
      // Set update fields
      Update().also { update -> data.forEach { (k, v) -> update.set(k, v) } }
    ).flatMap {
      if (it.matchedCount > 0) Mono.empty<Void>()
      else Mono.error(NotFoundException())
    }
  }

  override fun findDescendants(id: String): Flux<AttachmentDtoWithChildren> {
    return operations.aggregate(
      newAggregation(
        // Filter out the specified Attachment
        match(Criteria.where("id").`is`(id)),
        // Aggregate all descendants into an array into field "aggregate"
        graphLookup("st_attachment")
          .startWith("id")
          .connectFrom("_id")
          .connectTo("upperId")
          .`as`("aggregate"),
        project("aggregate")
      ),
      AttachmentPo::class.java,
      AttachmentDescendantsDtoWithUpper::class.java
    )
      .singleOrEmpty().map(AttachmentDescendantsDtoWithUpper::dtoWithChildren).flatMapIterable { it.children!! }
  }

  override fun getFullPath(id: String): Mono<String> {
    return operations.aggregate(
      newAggregation(
        // Filter out the specified Attachment
        match(Criteria.where("id").`is`(id)),
        // Aggregate all upper(including itself) into an array into field "aggregate"
        graphLookup("st_attachment")
          .startWith("id")
          .connectFrom("upperId")
          .connectTo("_id")
          .`as`("aggregate"),
        project("aggregate")
      ),
      AttachmentPo::class.java, AttachmentUppersPath::class.java
    )
      .singleOrEmpty().map(AttachmentUppersPath::fullPath)
  }

  @Suppress("UNCHECKED_CAST")
  override fun get(id: String): Mono<Attachment> {
    return repository.findById(id) as Mono<Attachment>
  }

  @Suppress("UNCHECKED_CAST")
  override fun find(pageable: Pageable): Mono<Page<Attachment>> {
    val query = Query().with(pageable)
    val zip: Mono<Page<Attachment>> = Mono.zip(
      operations.find(query, AttachmentPo::class.java).collectList() as Mono<List<Attachment>>,
      operations.count(query, AttachmentPo::class.java)
    ) { content, total -> PageImpl(content, pageable, total) }
    return zip.defaultIfEmpty(Page.empty<Attachment>(pageable))
  }

  @Suppress("UNCHECKED_CAST")
  override fun find(puid: String, upperId: String?): Flux<Attachment> {
    val condition = Criteria.where("puid").`is`(puid)
    if (null != upperId) condition.and("upperId").`is`(upperId)
    return operations.find(
      Query.query(condition).with(Sort.by(Sort.Direction.DESC, "createOn")),
      AttachmentPo::class.java
    ) as Flux<Attachment>
  }

  override fun save(vararg attachments: Attachment): Mono<Void> {
    return if (attachments.isEmpty()) Mono.empty()
    else repository.saveAll(attachments.asIterable().map { AttachmentPo.from(it) }).then()
  }

  override fun delete(vararg ids: String): Flux<String> {
    return if (ids.isEmpty()) Flux.empty()
    else
    // 1. Query the full path of the attachments
      operations.aggregate(
        newAggregation(
          match(Criteria.where("id").`in`(*ids)),
          graphLookup("st_attachment")
            .startWith("id").connectFrom("upperId").connectTo("_id").`as`("aggregate"),
          project("aggregate")
        ),
        AttachmentPo::class.java,
        AttachmentUppersPath::class.java
      )
        .map(AttachmentUppersPath::fullPath).collectList()
        // 2. Delete attachments
        .delayUntil { fullPath ->
          if (fullPath.isEmpty()) Mono.empty<Void>()
          else
          // 2.1. Query the attachments descendants
            operations.aggregate(
              newAggregation(
                match(Criteria.where("id").`in`(*ids)),
                graphLookup("st_attachment")
                  .startWith("id").connectFrom("_id").connectTo("upperId").`as`("aggregate"),
                project("aggregate")
              ),
              AttachmentPo::class.java,
              AttachmentDescendantsId::class.java
            )
              .map(AttachmentDescendantsId::descendants).flatMapIterable { it }.collectList()
              // 2.2. Delete attachments and theirs descendants
              .flatMap {
                operations.remove<AttachmentPo>(
                  Query.query(Criteria.where("id").`in`(*it.toSet().toTypedArray()))
                )
              }
        }.flatMapIterable { it }
  }
}