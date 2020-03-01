package tech.simter.file.impl.dao.jpa

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import reactor.kotlin.test.test
import tech.simter.file.core.AttachmentDao
import tech.simter.file.impl.dao.jpa.TestHelper.randomAttachmentPo
import tech.simter.file.test.TestHelper.randomAttachmentId
import tech.simter.reactive.test.jpa.ReactiveDataJpaTest
import tech.simter.reactive.test.jpa.TestEntityManager
import tech.simter.util.AssertUtils

/**
 * Test [AttachmentDao.findDescendants].
 *
 * @author RJ
 */
@SpringJUnitConfig(UnitTestConfiguration::class)
@ReactiveDataJpaTest
class FindDescendantsMethodImplTest @Autowired constructor(
  val rem: TestEntityManager,
  val repository: AttachmentJpaRepository,
  val dao: AttachmentDao
) {
  @Test
  fun `upper not exists`() {
    dao.findDescendants(randomAttachmentId()).test().verifyComplete()
  }

  @Test
  fun `upper has no descendants`() {
    // prepare data
    val po = randomAttachmentPo()
    rem.persist(po)

    // invoke and verify
    dao.findDescendants(po.id).test().verifyComplete()
  }

  @Test
  fun `upper has descendants`() {
    // clean
    repository.deleteAll()

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
    rem.persist(po100, po110, po111, po112, po120, po121, po122, po123)

    // invoke and verify
    dao.findDescendants(po100.id)
      .test()
      .consumeNextWith { actual ->
        AssertUtils.assertSamePropertyHasSameValue(po110, actual)
        Assertions.assertThat(actual.children).asList().hasSize(2)
        val children = actual.children!!
        AssertUtils.assertSamePropertyHasSameValue(po111, children[0])
        AssertUtils.assertSamePropertyHasSameValue(po112, children[1])
      }
      .consumeNextWith { actual ->
        AssertUtils.assertSamePropertyHasSameValue(po120, actual)
        Assertions.assertThat(actual.children).asList().hasSize(3)
        val children = actual.children!!
        AssertUtils.assertSamePropertyHasSameValue(po121, children[0])
        AssertUtils.assertSamePropertyHasSameValue(po122, children[1])
        AssertUtils.assertSamePropertyHasSameValue(po123, children[2])
      }
      .verifyComplete()
  }
}