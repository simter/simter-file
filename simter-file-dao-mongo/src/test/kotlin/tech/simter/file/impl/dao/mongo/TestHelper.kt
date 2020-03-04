package tech.simter.file.impl.dao.mongo

import reactor.kotlin.test.test
import tech.simter.file.impl.dao.mongo.po.AttachmentPo
import tech.simter.file.test.TestHelper
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit.SECONDS

/**
 * provide public method for test
 *
 * @author RJ
 */
object TestHelper {
  fun cleanDatabase(repository: AttachmentReactiveRepository) {
    repository.deleteAll()
      .test()
      .verifyComplete()
  }

  fun randomAttachmentPo(
    ts: OffsetDateTime = OffsetDateTime.now().truncatedTo(SECONDS)
  ): AttachmentPo {
    return AttachmentPo.from(TestHelper.randomAttachment(ts = ts))
  }
}