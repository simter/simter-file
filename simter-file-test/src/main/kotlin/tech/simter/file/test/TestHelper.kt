package tech.simter.file.test

import tech.simter.file.impl.domain.AttachmentImpl
import tech.simter.reactive.context.SystemContext
import tech.simter.util.RandomUtils.randomInt
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit.SECONDS
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

  /** Create a random string with a limit length */
  fun randomString(len: Int = 36): String {
    return UUID.randomUUID().toString().let {
      if (len < 36) it.substring(0, len)
      else it
    }
  }

  /** Create a random attachment identity */
  fun randomAttachmentId(): String {
    return UUID.randomUUID().toString()
  }

  /** Create a random attachment type */
  fun randomAttachmentType(): String {
    return randomString(3)
  }

  /** Create an attachment instance with random property values */
  fun randomAttachment(
    id: String = randomAttachmentId(),
    path: String = randomString(),
    name: String = randomString(6),
    type: String = randomAttachmentType(),
    size: Long = randomInt().toLong(),
    puid: String = randomString(6),
    upperId: String = randomAttachmentId(),
    ts: OffsetDateTime = OffsetDateTime.now().truncatedTo(SECONDS)
  ): AttachmentImpl {
    val user = randomString(6)
    return AttachmentImpl(
      id = id,
      path = path,
      name = name,
      type = type,
      size = size,
      puid = puid,
      upperId = upperId,
      createOn = ts,
      creator = user,
      modifyOn = ts,
      modifier = user
    )
  }
}