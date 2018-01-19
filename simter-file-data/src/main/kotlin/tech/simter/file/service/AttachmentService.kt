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
   * Create a given attachment entity. Use the returned instance for further operations as the save operation might have
   * changed the attachment instance completely.
   *
   * @param attachment must not be {@literal null}.
   * @return {@link Mono} emitting the saved attachment.
   * @throws IllegalArgumentException in case the given {@code entity} is {@literal null}.
   */
  fun create(attachment: Mono<Attachment>): Mono<Attachment>
}