package tech.simter.file.core

import reactor.core.publisher.Mono
import java.time.OffsetDateTime
import java.util.*

/**
 * The file id generator.
 *
 * @author RJ
 */
@FunctionalInterface
interface FileIdGenerator {
  /**
   * Generate a new file id within the [ts] and [uuid] context.
   *
   * @param[ts] the timestamp when the caller call this method
   * @param[uuid] the unique id generated by the caller
   */
  fun nextId(
    ts: Optional<OffsetDateTime> = Optional.empty(),
    uuid: Optional<UUID> = Optional.empty()
  ): Mono<String>
}
