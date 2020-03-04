package tech.simter.file.impl.dao.r2dbc

import reactor.core.publisher.Mono
import tech.simter.file.impl.dao.r2dbc.po.AttachmentPo
import tech.simter.file.test.TestHelper.randomAttachment
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit.SECONDS

object TestHelper {
  fun cleanDatabase(repository: AttachmentRepository): Mono<Void> {
    return repository.deleteAll()
  }

  fun randomAttachmentPo(
    ts: OffsetDateTime = OffsetDateTime.now().truncatedTo(SECONDS)
  ): AttachmentPo {
    return AttachmentPo.from(randomAttachment(ts = ts))
  }
}