package tech.simter.file.impl.dao.r2dbc

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import tech.simter.file.core.AttachmentDao
import tech.simter.file.core.domain.Attachment
import tech.simter.file.core.domain.AttachmentDto4Zip
import tech.simter.file.core.domain.AttachmentDtoWithChildren
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

  override fun find(pageable: Pageable): Mono<Page<Attachment>> {
    TODO("not implemented")
  }

  override fun find(puid: String, upperId: String?): Flux<Attachment> {
    TODO("not implemented")
  }

  override fun save(vararg attachments: Attachment): Mono<Void> {
    TODO("not implemented")
  }

  override fun delete(vararg ids: String): Flux<String> {
    TODO("not implemented")
  }

  override fun getFullPath(id: String): Mono<String> {
    TODO("not implemented")
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