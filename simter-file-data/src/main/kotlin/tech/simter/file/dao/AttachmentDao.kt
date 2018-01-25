package tech.simter.file.dao

import org.springframework.data.repository.Repository
import reactor.core.publisher.Mono
import tech.simter.file.po.Attachment

/**
 * Interface for generic CRUD operations on the attachment.
 *
 * @author cjw
 */
interface AttachmentDao : Repository<Attachment, String> {
  /**
   *  Retrieves an [Attachment] by its id.
   *
   *  @param id the id for matching.
   *  @return [Mono] emitting the [Attachment] with the given id or [Mono.empty] if none found.
   */
  fun findById(id: String): Mono<Attachment>

  /**
   * Save a given [Attachment].
   *
   * @param entity the attachment to save
   * @return [Mono] emitting the saved attachment
   */
  fun save(entity: Attachment): Mono<Attachment>
}