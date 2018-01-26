package tech.simter.file.service

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
   * Create a given attachment.
   *
   * @param attachment the attachment to save
   * @return [Mono] emitting the saved attachment
   */
  fun create(attachment: Mono<Attachment>): Mono<Attachment>
}