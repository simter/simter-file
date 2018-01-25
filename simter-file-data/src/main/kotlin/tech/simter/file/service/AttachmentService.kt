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
   * Create a given attachment.
   *
   * @param attachment the attachment to save
   * @return [Mono] emitting the saved attachment
   */
  fun create(attachment: Mono<Attachment>): Mono<Attachment>
}