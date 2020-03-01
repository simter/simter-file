package tech.simter.file.impl.dao.jpa

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import reactor.kotlin.test.test
import tech.simter.file.core.AttachmentDao
import tech.simter.file.impl.dao.jpa.po.AttachmentPo
import tech.simter.file.test.TestHelper.randomAttachmentId
import tech.simter.reactive.test.jpa.ReactiveDataJpaTest
import tech.simter.reactive.test.jpa.TestEntityManager
import java.time.OffsetDateTime

/**
 * Test [AttachmentDao.getFullPath].
 *
 * @author RJ
 */
@SpringJUnitConfig(UnitTestConfiguration::class)
@ReactiveDataJpaTest
class GetFullPathMethodImplTest @Autowired constructor(
  val rem: TestEntityManager,
  val dao: AttachmentDao
) {
  @Test
  fun `entity not exists`() {
    dao.getFullPath(randomAttachmentId()).test().verifyComplete()
  }

  @Test
  fun `get it`() {
    // prepare data
    val now = OffsetDateTime.now()
    val po1 = AttachmentPo(id = randomAttachmentId(), path = "data-1", name = "Sample-1", type = ":d", size = 123,
      createOn = now, creator = "Simter", modifyOn = now, modifier = "Simter", upperId = null)
    val po2 = AttachmentPo(id = randomAttachmentId(), path = "data-2", name = "Sample-2", type = ":d", size = 123,
      createOn = now, creator = "Simter", modifyOn = now, modifier = "Simter", upperId = po1.id)
    val po3 = AttachmentPo(id = randomAttachmentId(), path = "data-3", name = "Sample-3", type = ":d", size = 123,
      createOn = now, creator = "Simter", modifyOn = now, modifier = "Simter", upperId = po2.id)
    rem.persist(po1, po2, po3)

    // invoke and verify
    dao.getFullPath(po3.id)
      .test()
      .expectNext(listOf(po1, po2, po3).joinToString("/") { it.path }).verifyComplete()
  }
}