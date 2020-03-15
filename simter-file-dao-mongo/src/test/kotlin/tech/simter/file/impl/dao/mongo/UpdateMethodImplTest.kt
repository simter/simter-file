package tech.simter.file.impl.dao.mongo

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import reactor.kotlin.test.test
import tech.simter.exception.NotFoundException
import tech.simter.file.impl.dao.mongo.TestHelper.randomAttachmentPo
import tech.simter.file.impl.domain.AttachmentUpdateInfoImpl
import tech.simter.file.test.TestHelper.randomAttachmentId

/**
 * Test [AttachmentDaoImpl.update].
 *
 * @author RJ
 */
@SpringJUnitConfig(UnitTestConfiguration::class)
@DataMongoTest
class UpdateMethodImplTest @Autowired constructor(
  private val dao: AttachmentDao,
  private val repository: AttachmentReactiveRepository
) {
  @Test
  fun `update not exists`() {
    // prepare data
    val dto = AttachmentUpdateInfoImpl().apply {
      name = "newName"
      path = "/new-data"
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
    }

    // do update
    dao.update(po.id, dto.data).test().verifyComplete()

    // verify updated
    repository.findById(po.id)
      .test()
      .expectNext(po.copy(name = dto.name!!, path = dto.path!!))
      .verifyComplete()
  }
}