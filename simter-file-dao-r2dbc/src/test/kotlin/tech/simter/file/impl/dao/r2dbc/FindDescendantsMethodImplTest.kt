package tech.simter.file.impl.dao.r2dbc

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import reactor.kotlin.test.test
import tech.simter.file.core.AttachmentDao
import tech.simter.file.core.domain.AttachmentDtoWithChildren
import tech.simter.file.impl.dao.r2dbc.TestHelper.randomAttachmentId
import tech.simter.file.impl.dao.r2dbc.po.AttachmentPo
import java.time.OffsetDateTime

/**
 * Test [AttachmentDaoImpl.findDescendants].
 *
 * @author RJ
 */
@SpringBootTest(classes = [UnitTestConfiguration::class])
class FindDescendantsMethodImplTest @Autowired constructor(
  private val dao: AttachmentDao,
  private val repository: AttachmentRepository
) {
  @Test
  fun notFoundDescendants() {
    // prepare data
    val now = OffsetDateTime.now()
    val po = AttachmentPo(randomAttachmentId(), "/data1", "Sample1", "png",
      123, now, "Simter", now, "Simter")
    repository.save(po).test().expectNextCount(1).verifyComplete()

    // invoke and verify
    // none attachment
    dao.findDescendants(randomAttachmentId()).test().verifyComplete()
    // none descendants
    dao.findDescendants(po.id).test().verifyComplete()
  }

  @Test
  fun findDescendants() {
    // prepare data
    //            po100
    //       /            \
    //    po110          po120
    //   /     \      /    |     \
    // po111 po112  po121 po122 po123
    val now = OffsetDateTime.now()
    val po100 = AttachmentPo(id = "100", path = "data100", name = "Sample100", type = "png",
      size = 123, createOn = now, creator = "Simter", modifyOn = now, modifier = "Simter", upperId = null)
    val po110 = AttachmentPo(id = "110", path = "data110", name = "Sample110", type = "png",
      size = 123, createOn = now, creator = "Simter", modifyOn = now, modifier = "Simter", upperId = "100")
    val po120 = AttachmentPo(id = "120", path = "data120", name = "Sample120", type = "png",
      size = 123, createOn = now, creator = "Simter", modifyOn = now, modifier = "Simter", upperId = "100")
    val po111 = AttachmentPo(id = "111", path = "data111", name = "Sample111", type = "png",
      size = 123, createOn = now, creator = "Simter", modifyOn = now, modifier = "Simter", upperId = "110")
    val po112 = AttachmentPo(id = "112", path = "data112", name = "Sample112", type = "png",
      size = 123, createOn = now, creator = "Simter", modifyOn = now, modifier = "Simter", upperId = "110")
    val po121 = AttachmentPo(id = "121", path = "data121", name = "Sample121", type = "png",
      size = 123, createOn = now, creator = "Simter", modifyOn = now, modifier = "Simter", upperId = "120")
    val po122 = AttachmentPo(id = "122", path = "data122", name = "Sample122", type = "png",
      size = 123, createOn = now, creator = "Simter", modifyOn = now, modifier = "Simter", upperId = "120")
    val po123 = AttachmentPo(id = "123", path = "data123", name = "Sample123", type = "png",
      size = 123, createOn = now, creator = "Simter", modifyOn = now, modifier = "Simter", upperId = "120")
    val poList = listOf(po100, po110, po111, po112, po120, po121, po122, po123)
    repository.saveAll(poList).test().expectNextCount(poList.size.toLong()).verifyComplete()

    // invoke and verify
    dao.findDescendants(po100.id).test().consumeNextWith { actual ->
      assertEquals(AttachmentDtoWithChildren().copy(po110).getOwnData(), actual.getOwnData())
      assertEquals(
        AttachmentDtoWithChildren().copy(po111).getOwnData(),
        actual.children!!.first { it.id == po111.id }.getOwnData()
      )
      assertEquals(
        AttachmentDtoWithChildren().copy(po112).getOwnData(),
        actual.children!!.first { it.id == po112.id }.getOwnData()
      )
    }.consumeNextWith { actual ->
      assertEquals(AttachmentDtoWithChildren().copy(po120).getOwnData(), actual.getOwnData())
      assertEquals(AttachmentDtoWithChildren().copy(po121).getOwnData(), actual.children!![0].getOwnData())
      assertEquals(AttachmentDtoWithChildren().copy(po122).getOwnData(), actual.children!![1].getOwnData())
      assertEquals(AttachmentDtoWithChildren().copy(po123).getOwnData(), actual.children!![2].getOwnData())
    }.verifyComplete()
  }
}