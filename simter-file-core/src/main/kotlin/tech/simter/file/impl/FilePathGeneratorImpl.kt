package tech.simter.file.impl

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import tech.simter.file.MODULE_SEPARATOR
import tech.simter.file.core.FilePathGenerator
import tech.simter.file.core.FileDescriber
import tech.simter.file.timestampId
import java.nio.file.Path
import java.nio.file.Paths
import java.time.OffsetDateTime
import java.util.*

/**
 * The default file path generator.
 *
 * This implementation always return a relative path that relative to basePath.
 *
 * @author RJ
 */
@Component
class FilePathGeneratorImpl @Autowired constructor(
  @Value("\${simter-file.timestamp-id-limited-uuid-len: 6}")
  private val limitedUuidLen: Int
) : FilePathGenerator {
  /**
   * This implementation always return a relative path that relative to basePath.
   *
   * The return path is '$module/${yyyyMMddTHHmmssSSSS}-${randomUUIDPrefix}'
   */
  override fun resolve(
    describer: FileDescriber,
    ts: Optional<OffsetDateTime>,
    uuid: Optional<UUID>
  ): Path {
    return Paths.get(
      // remove prefix '/' to make sure it is a relative path
      if (describer.module.startsWith(MODULE_SEPARATOR)) describer.module.substring(1)
      else describer.module,
      timestampId(
        ts = ts,
        uuid = uuid,
        uuidLen = limitedUuidLen
      ) + "." + describer.type
    )
  }
}
