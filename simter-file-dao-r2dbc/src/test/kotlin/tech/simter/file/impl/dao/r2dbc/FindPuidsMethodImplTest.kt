package tech.simter.file.impl.dao.r2dbc

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import reactor.kotlin.test.test
import tech.simter.file.core.AttachmentDao
import tech.simter.file.impl.dao.r2dbc.po.AttachmentPo
import tech.simter.file.test.TestHelper.randomAttachmentId
import java.time.OffsetDateTime
import java.util.*

/**
 * Test [AttachmentDaoImpl.findPuids].
 *
 * @author RJ
 */
@SpringBootTest(classes = [UnitTestConfiguration::class])
class FindPuidsMethodImplTest @Autowired constructor(
  private val dao: AttachmentDao,
  private val repository: AttachmentRepository
) {
  @Test
  fun test() {
    // prepare data
    val now = OffsetDateTime.now()
    val pos = List(10) {
      AttachmentPo(randomAttachmentId(), "/data", "Sample", "png",
        123, now, "Simter", now, "Simter", puid = if (it < 2) null else "puid${it / 2}")
    }
    repository.saveAll(pos).test().expectNextCount(pos.size.toLong()).verifyComplete()

    // invoke and verify
    dao.findPuids(*pos.map { it.id }.toTypedArray())
      .collectList().map { it.toSet() }
      .test()
      .expectNext(pos.map { Optional.ofNullable(it.puid) }.toSet())
      .verifyComplete()
  }
}