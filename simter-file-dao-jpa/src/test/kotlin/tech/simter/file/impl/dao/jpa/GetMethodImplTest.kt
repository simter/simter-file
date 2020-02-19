package tech.simter.file.impl.dao.jpa

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import reactor.kotlin.test.test
import tech.simter.file.core.AttachmentDao
import tech.simter.file.impl.dao.jpa.TestHelper.randomAttachmentId
import tech.simter.file.impl.dao.jpa.TestHelper.randomAttachmentPo
import tech.simter.reactive.test.jpa.ReactiveDataJpaTest
import tech.simter.reactive.test.jpa.TestEntityManager

/**
 * Test [AttachmentDao.get].
 *
 * @author RJ
 */
@SpringJUnitConfig(UnitTestConfiguration::class)
@ReactiveDataJpaTest
class GetMethodImplTest @Autowired constructor(
  val rem: TestEntityManager,
  val dao: AttachmentDao
) {
  @Test
  fun `get nothing`() {
    dao.get(randomAttachmentId()).test().verifyComplete()
  }

  @Test
  fun `get it`() {
    // prepare data
    val po = randomAttachmentPo()
    rem.persist(po)

    // verify exists
    dao.get(po.id)
      .test()
      .expectNext(po)
      .verifyComplete()
  }
}