package tech.simter.file.impl.dao.jpa

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import reactor.kotlin.test.test
import tech.simter.file.core.AttachmentDao
import tech.simter.file.impl.dao.jpa.TestHelper.randomAttachmentPo
import tech.simter.file.impl.dao.jpa.po.AttachmentPo
import tech.simter.file.test.TestHelper.randomAttachmentId
import tech.simter.reactive.test.jpa.ReactiveDataJpaTest
import tech.simter.reactive.test.jpa.TestEntityManager
import java.time.OffsetDateTime

/**
 * Test [AttachmentDao.save].
 *
 * @author RJ
 */
@SpringJUnitConfig(UnitTestConfiguration::class)
@ReactiveDataJpaTest
class SaveMethodImplTest @Autowired constructor(
  val rem: TestEntityManager,
  val dao: AttachmentDao
) {
  @Test
  fun `save nothing`() {
    dao.save().test().verifyComplete()
  }

  @Test
  fun `save one`() {
    // invoke
    val po = randomAttachmentPo()
    val actual = dao.save(po)

    // verify result
    actual.test().verifyComplete()

    // verify saved
    assertEquals(po, rem.find(AttachmentPo::class.java, po.id).get())
  }

  @Test
  fun `save multiple`() {
    val now = OffsetDateTime.now()
    val pos = (1..3).map {
      AttachmentPo(
        id = randomAttachmentId(),
        path = "/data$it",
        name = "Sample-$it",
        type = "png",
        size = 123,
        createOn = now,
        creator = "Simter",
        modifyOn = now,
        modifier = "Simter"
      )
    }
    val actual = dao.save(*pos.toTypedArray())

    // verify result
    actual.test().verifyComplete()

    // verify saved
    pos.forEach { assertEquals(it, rem.find(AttachmentPo::class.java, it.id).get()) }
  }
}