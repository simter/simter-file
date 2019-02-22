package tech.simter.file.dao.reactive.mongo

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
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
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import tech.simter.exception.NotFoundException
import tech.simter.file.dao.AttachmentDao
import tech.simter.file.dto.AttachmentDto4Zip
import tech.simter.file.dto.AttachmentDtoWithChildren
import tech.simter.file.po.Attachment
import java.util.*

/**
 * The Reactive MongoDB implementation of [AttachmentDao].
 *
 * @author RJ
 * @author zh
 */
@Component
class AttachmentDaoImpl @Autowired constructor(
  private val repository: AttachmentReactiveRepository,
  private val operations: ReactiveMongoOperations
) : AttachmentDao {
  override fun findPuids(vararg ids: String): Flux<Optional<String>> {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun findDescendentsZipPath(vararg ids: String): Flux<AttachmentDto4Zip> {
    return operations.aggregate(
      newAggregation(
        // Filter out the specified Attachment
        match(Criteria.where("id").`in`(*ids)),
        // Aggregate all upper(including itself) into an array into field "aggregate"
        graphLookup("st_attachment")
          .startWith("id").connectFrom("upperId").connectTo("_id").`as`("uppers"),
        // Aggregate all descendents into an array into field "aggregate"
        graphLookup("st_attachment")
          .startWith("id").connectFrom("_id").connectTo("upperId").`as`("descendents"),
        project("uppers", "descendents", "type")
      ),
      Attachment::class.java,
      AttachmentUppersWithDescendents::class.java
    )
      .collectList().flatMapIterable { it.convertToAttachmentDto4Zip() }
  }

  @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
  override fun update(id: String, data: Map<String, Any?>): Mono<Void> {
    return if (data.isEmpty()) Mono.empty()
    else operations.updateMulti(
      // Filter out the specified Attachment
      Query.query(Criteria.where("id").`is`(id)),
      // Set update fields
      Update().also { update ->
        data.forEach { k, v ->
          update.set(k, v)
        }
      },
      Attachment::class
    ).flatMap {
      if (it.matchedCount > 0) Mono.empty<Void>()
      else Mono.error(NotFoundException())
    }
  }

  override fun findDescendents(id: String): Flux<AttachmentDtoWithChildren> {
    return operations.aggregate(
      newAggregation(
        // Filter out the specified Attachment
        match(Criteria.where("id").`is`(id)),
        // Aggregate all descendents into an array into field "aggregate"
        graphLookup("st_attachment")
          .startWith("id").connectFrom("_id").connectTo("upperId").`as`("aggregate"),
        project("aggregate")
      ),
      Attachment::class.java,
      AttachmentDescendentsDtoWithUpper::class.java
    )
      .singleOrEmpty().map(AttachmentDescendentsDtoWithUpper::dtoWithChildren).flatMapIterable { it.children!! }
  }

  override fun getFullPath(id: String): Mono<String> {
    return operations.aggregate(
      newAggregation(
        // Filter out the specified Attachment
        match(Criteria.where("id").`is`(id)),
        // Aggregate all upper(including itself) into an array into field "aggregate"
        graphLookup("st_attachment")
          .startWith("id").connectFrom("upperId").connectTo("_id").`as`("aggregate"),
        project("aggregate")
      ),
      Attachment::class.java, AttachmentUppersPath::class.java
    )
      .singleOrEmpty().map(AttachmentUppersPath::fullPath)
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
        Attachment::class.java,
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
              Attachment::class.java,
              AttachmentDescendentsId::class.java
            )
              .map(AttachmentDescendentsId::descendents).flatMapIterable { it }.collectList()
              // 2.2. Delete attachments and theirs descendants
              .flatMap {
                operations.remove(
                  Query.query(Criteria.where("id").`in`(*it.toSet().toTypedArray())),
                  Attachment::class
                )
              }
        }.flatMapIterable { it }
  }
}