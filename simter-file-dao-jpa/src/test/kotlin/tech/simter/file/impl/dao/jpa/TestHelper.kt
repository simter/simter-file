package tech.simter.file.impl.dao.jpa

import tech.simter.file.impl.dao.jpa.po.AttachmentPo
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit.SECONDS
import java.util.*

/**
 * provide public method for test
 *
 * @author RJ
 */
object TestHelper {
  fun randomString(): String {
    return UUID.randomUUID().toString()
  }

  fun randomAttachmentId(): String {
    return UUID.randomUUID().toString()
  }

  fun randomAttachmentPo(
    ts: OffsetDateTime = OffsetDateTime.now().truncatedTo(SECONDS)
  ): AttachmentPo {
    return AttachmentPo(
      id = randomAttachmentId(),
      path = randomString().substring(0, 6),
      name = randomString().substring(0, 6),
      type = randomString().substring(0, 6),
      size = 1,
      createOn = ts.minusDays(1),
      creator = randomString().substring(0, 6),
      modifyOn = ts,
      modifier = randomString().substring(0, 6),
      puid = randomString().substring(0, 6),
      upperId = randomString()
    )
  }
}