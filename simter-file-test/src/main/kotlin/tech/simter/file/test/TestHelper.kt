package tech.simter.file.test

import tech.simter.file.core.FileStore
import tech.simter.file.timestampId
import tech.simter.reactive.context.SystemContext
import tech.simter.util.RandomUtils.randomInt
import java.nio.file.Paths
import java.time.OffsetDateTime
import java.util.*

/**
 * Some common tools for simter-file unit test.
 *
 * @author RJ
 */
object TestHelper {
  /** Create a random authenticated user */
  fun randomAuthenticatedUser(
    id: Int = randomInt(),
    account: String = randomString(),
    name: String = randomString()
  ): SystemContext.User {
    return SystemContext.User(id = id, account = account, name = name)
  }

  /** Create a random uuid string with a limit length */
  fun randomString(len: Int = 36): String {
    return UUID.randomUUID().toString().let {
      if (len < 36) it.substring(0, len)
      else it
    }
  }

  /** Create a random file module type */
  fun randomModuleValue(): String {
    return "/" + randomString(6) + "/"
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