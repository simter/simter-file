package tech.simter.file.impl

import tech.simter.file.impl.domain.AttachmentImpl
import tech.simter.reactive.context.SystemContext.User
import tech.simter.util.RandomUtils.randomInt
import tech.simter.util.RandomUtils.randomString
import java.time.OffsetDateTime
import java.util.*

object TestHelper {
  fun randomAttachmentId(): String {
    return UUID.randomUUID().toString()
  }

  fun randomAuthenticatedUser(
    id: Int = randomInt(),
    account: String = randomString(),
    name: String = randomString()
  ): User {
    return User(id = id, account = account, name = name)
  }

  fun randomAttachment(): AttachmentImpl {
    val now = OffsetDateTime.now()
    return AttachmentImpl(
      id = randomString(),
      path = randomString(),
      name = randomString(),
      type = randomString(),
      size = randomInt().toLong(),
      createOn = now.minusDays(1),
      creator = randomString(),
      modifyOn = now.minusDays(1),
      modifier = randomString(),
      puid = randomString(),
      upperId = randomString()
    )
  }
}