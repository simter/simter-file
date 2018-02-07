package tech.simter.file.service

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import reactor.core.publisher.Mono
import tech.simter.file.po.Attachment

/**
 * Interface for service of the attachment.
 *
 * @author cjw
 */
interface AttachmentService {
  /**
   *  Get an [Attachment] by its id.
   *
   *  @param id the id for matching.
   *  @return [Mono] emitting the [Attachment] with the given id or [Mono.empty] if none found.
   */
  fun get(id: String): Mono<Attachment>

  /**
   * Returns a [Page] of [Attachment]'s meeting the paging restriction provided in the [Pageable] object.
   *
   * @param pageable pageable options
   * @return [Mono] emitting a page of Attachments
   */
  fun find(pageable: Pageable): Mono<Page<Attachment>>

  /**
   * Create a given attachment.
   *
   * @param attachment the attachment to save
   * @return [Mono] emitting the saved attachment
   */
  fun create(attachment: Mono<Attachment>): Mono<Attachment>
}