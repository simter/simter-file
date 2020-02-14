package tech.simter.file.service

import tech.simter.file.po.Attachment
import tech.simter.reactive.context.SystemContext.User
import tech.simter.util.RandomUtils.randomInt
import tech.simter.util.RandomUtils.randomString
import java.time.OffsetDateTime

object TestUtils {
  fun randomAuthenticatedUser(
    id: Int = randomInt(),
    account: String = randomString(),
    name: String = randomString()
  ): User {
    return User(id = id, account = account, name = name)
  }

  fun randomAttachment(): Attachment {
    val now = OffsetDateTime.now()
    return Attachment(randomString(), randomString(), randomString(), randomString(), randomInt().toLong(), now.minusDays(1),
      randomString(), now.minusDays(1), randomString(), randomString(), randomString())
  }
}