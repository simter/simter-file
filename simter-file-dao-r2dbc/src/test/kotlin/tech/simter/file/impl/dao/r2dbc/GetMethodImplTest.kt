package tech.simter.file.impl.dao.r2dbc

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import reactor.kotlin.test.test
import tech.simter.file.core.AttachmentDao
import tech.simter.file.impl.dao.r2dbc.TestHelper.randomAttachmentId
import tech.simter.file.impl.dao.r2dbc.TestHelper.randomAttachmentPo

/**
 * Test [AttachmentDaoImpl.get]
 *
 * @author RJ
 */
@SpringBootTest(classes = [UnitTestConfiguration::class])
class GetMethodImplTest @Autowired constructor(
  private val repository: AttachmentRepository,
  private val dao: AttachmentDao
) {
  @Test
  fun `get nothing`() {
    dao.get(randomAttachmentId()).test().verifyComplete()
  }

  @Test
  fun `get it`() {
    // prepare data
    val po = randomAttachmentPo()
    repository.save(po).test().expectNextCount(1).verifyComplete()

    // verify exists
    dao.get(po.id)
      .test()
      .expectNext(po)
      .verifyComplete()
  }
}