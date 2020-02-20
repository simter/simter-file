package tech.simter.file.impl.dao.r2dbc

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import reactor.kotlin.test.test
import tech.simter.file.core.AttachmentDao
import tech.simter.file.impl.dao.r2dbc.TestHelper.randomAttachmentId
import tech.simter.file.impl.dao.r2dbc.TestHelper.randomAttachmentPo
import tech.simter.util.AssertUtils.assertSamePropertyHasSameValue

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
  fun `upper not exists`() {
    dao.findDescendants(randomAttachmentId()).test().verifyComplete()
  }

  @Test
  fun `upper has no descendants`() {
    // prepare data
    val po = randomAttachmentPo()
    repository.save(po).test().expectNextCount(1).verifyComplete()

    // invoke and verify
    dao.findDescendants(po.id).test().verifyComplete()
  }

  @Test
  fun `upper has descendants`() {
    // prepare data
    //            po100
    //       /            \
    //    po110          po120
    //   /     \      /    |     \
    // po111 po112  po121 po122 po123
    val base = randomAttachmentPo()
    val po100 = base.copy(id = "100", path = "data100", upperId = null)
    val po110 = base.copy(id = "110", path = "data110", upperId = "100")
    val po120 = base.copy(id = "120", path = "data120", upperId = "100")
    val po111 = base.copy(id = "111", path = "data111", upperId = "110")
    val po112 = base.copy(id = "112", path = "data112", upperId = "110")
    val po121 = base.copy(id = "121", path = "data121", upperId = "120")
    val po122 = base.copy(id = "122", path = "data122", upperId = "120")
    val po123 = base.copy(id = "123", path = "data123", upperId = "120")
    val poList = listOf(po100, po110, po111, po112, po120, po121, po122, po123)
    repository.saveAll(poList).test().expectNextCount(poList.size.toLong()).verifyComplete()

    // invoke and verify
    dao.findDescendants(po100.id)
      .test()
      .consumeNextWith { actual ->
        assertSamePropertyHasSameValue(po110, actual)
        assertThat(actual.children).asList().hasSize(2)
        val children = actual.children!!
        assertSamePropertyHasSameValue(po111, children[0])
        assertSamePropertyHasSameValue(po112, children[1])
      }
      .consumeNextWith { actual ->
        assertSamePropertyHasSameValue(po120, actual)
        assertThat(actual.children).asList().hasSize(3)
        val children = actual.children!!
        assertSamePropertyHasSameValue(po121, children[0])
        assertSamePropertyHasSameValue(po122, children[1])
        assertSamePropertyHasSameValue(po123, children[2])
      }
      .verifyComplete()
  }
}