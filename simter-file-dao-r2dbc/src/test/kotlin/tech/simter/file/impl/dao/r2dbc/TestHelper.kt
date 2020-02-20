package tech.simter.file.impl.dao.r2dbc

import org.springframework.data.r2dbc.core.DatabaseClient
import reactor.core.publisher.Mono
import tech.simter.file.TABLE_ATTACHMENT
import tech.simter.file.core.domain.AttachmentDtoWithChildren
import tech.simter.file.impl.dao.r2dbc.po.AttachmentPo
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit.SECONDS
import java.util.*

fun AttachmentDtoWithChildren.getOwnData(): Map<String, Any?> {
  return data.filter { it.key != "children" }
}

object TestHelper {
  fun cleanDatabase(repository: AttachmentRepository): Mono<Void> {
    return repository.deleteAll()
  }

  fun cleanDatabase(databaseClient: DatabaseClient): Mono<Void> {
    return databaseClient.delete().from(TABLE_ATTACHMENT).then()
  }

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