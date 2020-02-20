package tech.simter.file.impl.dao.jpa

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import reactor.kotlin.test.test
import tech.simter.exception.NotFoundException
import tech.simter.file.core.AttachmentDao
import tech.simter.file.impl.dao.jpa.TestHelper.randomAttachmentId
import tech.simter.file.impl.dao.jpa.po.AttachmentPo
import tech.simter.file.impl.domain.AttachmentUpdateInfoImpl
import tech.simter.reactive.test.jpa.ReactiveDataJpaTest
import tech.simter.reactive.test.jpa.TestEntityManager
import java.time.OffsetDateTime

/**
 * Test [AttachmentDao.update].
 *
 * @author RJ
 */
@SpringJUnitConfig(UnitTestConfiguration::class)
@ReactiveDataJpaTest
class UpdateMethodImplTest @Autowired constructor(
  val rem: TestEntityManager,
  val dao: AttachmentDao
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
    val now = OffsetDateTime.now()
    val po = AttachmentPo(randomAttachmentId(), "/data1", "Sample1", "png",
      123, now, "Simter", now, "Simter")
    rem.persist(po)
    val dto = AttachmentUpdateInfoImpl().apply {
      name = "newName"
      path = "/new-data"
    }

    // invoke and verify
    dao.update(po.id, dto.data).test().verifyComplete()
    assertEquals(po.copy(name = dto.name!!, path = dto.path!!), rem.find(AttachmentPo::class.java, po.id).get())
  }
}