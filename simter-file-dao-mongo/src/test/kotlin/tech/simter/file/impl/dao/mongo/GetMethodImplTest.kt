package tech.simter.file.impl.dao.mongo

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import reactor.kotlin.test.test
import tech.simter.file.impl.dao.mongo.TestHelper.randomAttachmentPo
import tech.simter.file.test.TestHelper.randomAttachmentId

/**
 * Test [AttachmentDaoImpl.get]
 *
 * @author RJ
 */
@SpringJUnitConfig(UnitTestConfiguration::class)
@DataMongoTest
class GetMethodImplTest @Autowired constructor(
  private val dao: AttachmentDao,
  private val repository: AttachmentReactiveRepository
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