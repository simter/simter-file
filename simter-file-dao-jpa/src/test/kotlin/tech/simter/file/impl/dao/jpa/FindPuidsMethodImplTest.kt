package tech.simter.file.impl.dao.jpa

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import reactor.kotlin.test.test
import tech.simter.file.core.AttachmentDao
import tech.simter.file.impl.dao.jpa.TestHelper.randomAttachmentId
import tech.simter.file.impl.dao.jpa.po.AttachmentPo
import tech.simter.reactive.test.jpa.ReactiveDataJpaTest
import tech.simter.reactive.test.jpa.TestEntityManager
import java.time.OffsetDateTime
import java.util.*

/**
 * Test [AttachmentDao.findPuids].
 *
 * @author RJ
 */
@SpringJUnitConfig(UnitTestConfiguration::class)
@ReactiveDataJpaTest
class FindPuidsMethodImplTest @Autowired constructor(
  val rem: TestEntityManager,
  val dao: AttachmentDao
) {
  @Test
  fun test() {
    // prepare data
    val now = OffsetDateTime.now()
    val pos = List(10) {
      AttachmentPo(randomAttachmentId(), "/data", "Sample", "png",
        123, now, "Simter", now, "Simter", puid = if (it < 2) null else "puid${it / 2}")
    }
    rem.persist(*pos.toTypedArray())

    // invoke and verify
    dao.findPuids(*pos.map { it.id }.toTypedArray())
      .collectList()
      .test()
      .expectNext(pos.map { it.puid }.toSet().map { Optional.ofNullable(it) })
      .verifyComplete()
  }
}