package tech.simter.file.dao

import org.springframework.data.repository.Repository
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import reactor.core.publisher.Mono
import tech.simter.file.po.Attachment

/**
 * Interface for generic CRUD operations on the attachment. See [ReactiveCrudRepository].
 * @author cjw
 */
interface AttachmentDao : Repository<Attachment, String> {
  /**
   * Save a given [Attachment].
   *
   * @param entity the attachment to save
   * @return [Mono] emitting the saved attachment
   */
  fun save(entity: Attachment): Mono<Attachment>
}