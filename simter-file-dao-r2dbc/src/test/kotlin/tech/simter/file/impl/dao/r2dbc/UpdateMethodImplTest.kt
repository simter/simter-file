package tech.simter.file.impl.dao.r2dbc

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import reactor.kotlin.test.test
import tech.simter.exception.NotFoundException
import tech.simter.file.core.AttachmentDao
import tech.simter.file.impl.dao.r2dbc.TestHelper.randomAttachmentPo
import tech.simter.file.impl.domain.AttachmentUpdateInfoImpl
import tech.simter.file.test.TestHelper.randomAttachmentId

/**
 * Test [AttachmentDaoImpl.update].
 *
 * @author RJ
 */
@SpringBootTest(classes = [UnitTestConfiguration::class])
class UpdateMethodImplTest @Autowired constructor(
  private val dao: AttachmentDao,
  private val repository: AttachmentRepository
) {
  @Test
  fun `update not exists`() {
    // prepare data
    val dto = AttachmentUpdateInfoImpl().apply {
      name = "newName"
      path = "/new-data"
      upperId = null
    }

    // invoke and verify
    dao.update(randomAttachmentId(), dto.data).test().verifyError(NotFoundException::class.java)
  }

  @Test
  fun `update it`() {
    // prepare data
    val po = randomAttachmentPo()
    repository.save(po).test().expectNextCount(1).verifyComplete()
    val dto = AttachmentUpdateInfoImpl().apply {
      name = "newName"
      path = "/new-data"
      upperId = null
    }

    // do update
    dao.update(po.id, dto.data).test().verifyComplete()

    // verify updated
    repository.findById(po.id)
      .test()
      .expectNext(po.copy(name = dto.name!!, path = dto.path!!, upperId = null))
      .verifyComplete()
  }
}