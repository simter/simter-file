package tech.simter.file.impl.dao.jpa

import tech.simter.file.impl.dao.jpa.po.AttachmentPo
import tech.simter.file.test.TestHelper.randomAttachment
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit.SECONDS

/**
 * provide public method for test
 *
 * @author RJ
 */
object TestHelper {
  fun randomAttachmentPo(
    ts: OffsetDateTime = OffsetDateTime.now().truncatedTo(SECONDS)
  ): AttachmentPo {
    return AttachmentPo.from(randomAttachment(ts = ts))
  }
}