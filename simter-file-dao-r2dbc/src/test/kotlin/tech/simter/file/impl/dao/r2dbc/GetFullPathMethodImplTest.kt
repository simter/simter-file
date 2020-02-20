package tech.simter.file.impl.dao.r2dbc

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import reactor.kotlin.test.test
import tech.simter.file.core.AttachmentDao
import tech.simter.file.impl.dao.r2dbc.TestHelper.randomAttachmentId
import tech.simter.file.impl.dao.r2dbc.po.AttachmentPo
import java.time.OffsetDateTime

/**
 * Test [AttachmentDao.getFullPath].
 *
 * @author RJ
 */
@SpringBootTest(classes = [UnitTestConfiguration::class])
class GetFullPathMethodImplTest @Autowired constructor(
  private val dao: AttachmentDao,
  private val repository: AttachmentRepository
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
    val poList = listOf(po1, po2, po3)
    repository.saveAll(poList).test().expectNextCount(poList.size.toLong()).verifyComplete()

    // invoke and verify
    dao.getFullPath(po3.id)
      .test()
      .expectNext(poList.joinToString("/") { it.path }).verifyComplete()
  }
}