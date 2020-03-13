package tech.simter.file.impl

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import tech.simter.file.core.FileIdGenerator
import tech.simter.file.timestampId
import java.time.OffsetDateTime
import java.util.*

/**
 * The default file id generator.
 *
 * @author RJ
 */
@Component
class FileIdGeneratorImpl @Autowired constructor(
  @Value("\${simter-file.timestamp-id-limited-uuid-len: 6}")
  private val limitedUuidLen: Int
) : FileIdGenerator {
  /**
   * Generate a string with timestamp as prefix and limit-len uuid as suffix.
   *
   * The format is '${yyyyMMddTHHmmssSSSS}-${randomUUIDPrefix}'
   */
  override fun nextId(ts: Optional<OffsetDateTime>, uuid: Optional<UUID>): Mono<String> {
    return Mono.just(timestampId(
      ts = ts,
      uuid = uuid,
      uuidLen = limitedUuidLen
    ))
  }
}
