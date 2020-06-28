package tech.simter.file.test

import tech.simter.file.core.FileStore
import tech.simter.file.timestampId
import tech.simter.util.RandomUtils.randomInt
import tech.simter.util.RandomUtils.randomString
import java.nio.file.Paths
import java.time.OffsetDateTime

/**
 * Some common tools for simter-file unit test.
 *
 * @author RJ
 */
object TestHelper {
  /** Create a random file module type */
  fun randomModuleValue(): String {
    return "/test/" + randomString(6) + "/"
  }

  /** Create a random file identity */
  fun randomFileId(): String {
    return timestampId()
  }

  /** Create a random fileStore instance */
  fun randomFileStore(
    id: String = timestampId(),
    module: String = randomModuleValue(),
    name: String = randomString(8),
    type: String = "xyz",
    size: Long = randomInt().toLong(),
    ts: OffsetDateTime = OffsetDateTime.now()
  ): FileStore {
    val path = Paths.get(
      if (module.startsWith("/")) module.substring(1) else module,
      "$name.$type"
    ).toString()
    val creator = randomString(4)
    return FileStore.Impl(
      id = id,
      module = module,
      path = path,
      name = name,
      type = type,
      size = size,
      createOn = ts,
      creator = creator,
      modifyOn = ts,
      modifier = creator
    )
  }
}