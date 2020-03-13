package tech.simter.file.impl

import tech.simter.file.core.FileStore
import tech.simter.reactive.context.SystemContext.User
import tech.simter.util.RandomUtils.randomInt
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
import java.util.*

object TestHelper {
  fun randomAuthenticatedUser(
    id: Int = randomInt(),
    account: String = randomString(),
    name: String = randomString()
  ): User {
    return User(id = id, account = account, name = name)
  }

  /** Create a random string with a limit length */
  fun randomString(len: Int = 36): String {
    return UUID.randomUUID().toString().let {
      if (len < 36) it.substring(0, len)
      else it
    }
  }

  /** Create a random file id */
  fun randomFileId(): String {
    return randomString()
  }

  /** Create a random file module type */
  fun randomModuleValue(): String {
    return "/" + randomString(6) + "/"
  }

  /** Create a random fileStore instance */
  fun randomFileStore(
    module: String = randomModuleValue(),
    ts: OffsetDateTime = OffsetDateTime.now().truncatedTo(ChronoUnit.SECONDS)
  ): FileStore.Impl {
    return FileStore.Impl(
      id = randomString(),
      module = module,
      path = randomString(),
      name = randomString(),
      type = randomString(),
      size = randomInt().toLong(),
      createOn = ts.minusDays(1),
      creator = randomString(),
      modifyOn = ts.minusDays(1),
      modifier = randomString()
    )
  }
}