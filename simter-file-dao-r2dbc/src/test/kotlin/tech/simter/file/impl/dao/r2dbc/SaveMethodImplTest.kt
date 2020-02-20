package tech.simter.file.impl.dao.r2dbc

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import reactor.kotlin.test.test
import tech.simter.file.core.AttachmentDao
import tech.simter.file.impl.dao.r2dbc.TestHelper.randomAttachmentId
import tech.simter.file.impl.dao.r2dbc.TestHelper.randomAttachmentPo

/**
 * Test [AttachmentDaoImpl.save].
 *
 * @author RJ
 */
@SpringBootTest(classes = [UnitTestConfiguration::class])
class SaveMethodImplTest @Autowired constructor(
  private val dao: AttachmentDao,
  private val repository: AttachmentRepository
) {
  @Test
  fun `save nothing`() {
    dao.save().test().verifyComplete()
  }

  @Test
  fun `save one`() {
    // do save
    val po = randomAttachmentPo()
    dao.save(po).test().verifyComplete()

    // verify saved
    repository.findById(po.id).test().expectNext(po)
  }

  @Test
  fun `save multiple`() {
    // do save
    val base = randomAttachmentPo()
    val poList = (1..3).map { base.copy(id = randomAttachmentId()) }
    dao.save(*poList.toTypedArray()).test().verifyComplete()

    // verify saved
    repository.findAllById(poList.map { it.id })
      .collectList()
      .test()
      .assertNext { actualList ->
        assertEquals(poList.size, actualList.size)
        poList.forEach { actual ->
          assertEquals(poList.first { it.id == actual.id }, actual)
        }
      }
      .verifyComplete()
  }
}