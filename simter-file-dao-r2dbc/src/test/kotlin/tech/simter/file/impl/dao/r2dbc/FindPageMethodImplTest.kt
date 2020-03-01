package tech.simter.file.impl.dao.r2dbc

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import reactor.kotlin.test.test
import tech.simter.file.core.AttachmentDao
import tech.simter.file.impl.dao.r2dbc.TestHelper.randomAttachmentPo
import tech.simter.file.test.TestHelper.randomAttachmentId
import java.time.OffsetDateTime

/**
 * Test [AttachmentDaoImpl.find].
 *
 * @author RJ
 */
@SpringBootTest(classes = [UnitTestConfiguration::class])
class FindPageMethodImplTest @Autowired constructor(
  private val repository: AttachmentRepository,
  private val dao: AttachmentDao
) {
  @Test
  fun `found nothing`() {
    // clean
    repository.deleteAll().test().verifyComplete()

    // invoke
    dao.find(PageRequest.of(0, 25))
      .test()
      .consumeNextWith { page ->
        assertTrue(page.content.isEmpty())
        assertEquals(0, page.number)
        assertEquals(25, page.size)
        assertEquals(0, page.totalPages)
        assertEquals(0, page.totalElements)
      }
      .verifyComplete()
  }

  @Test
  fun `found something`() {
    // clean
    repository.deleteAll().test().verifyComplete()

    // prepare data
    val now = OffsetDateTime.now()
    val base = randomAttachmentPo()
    val po1 = base.copy(id = randomAttachmentId(), createOn = now.minusDays(1))
    val po2 = base.copy(id = randomAttachmentId(), createOn = now)
    repository.saveAll(listOf(po1, po2)).test().expectNextCount(2).verifyComplete()

    // invoke
    val actual = dao.find(PageRequest.of(0, 25, Sort.by(Sort.Direction.DESC, "createOn")))

    // verify
    actual.test()
      .consumeNextWith { page ->
        assertEquals(0, page.number)
        assertEquals(25, page.size)
        assertEquals(1, page.totalPages)
        assertEquals(2L, page.totalElements)
        assertEquals(2, page.content.size)
        assertEquals(po2, page.content[0])
        assertEquals(po1, page.content[1])
      }
      .verifyComplete()
  }
}