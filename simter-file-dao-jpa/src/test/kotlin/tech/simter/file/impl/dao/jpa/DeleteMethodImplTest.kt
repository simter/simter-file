package tech.simter.file.impl.dao.jpa

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import reactor.kotlin.test.test
import tech.simter.file.core.AttachmentDao
import tech.simter.file.impl.dao.jpa.TestHelper.randomAttachmentId
import tech.simter.file.impl.dao.jpa.TestHelper.randomAttachmentPo
import tech.simter.file.impl.dao.jpa.po.AttachmentPo
import tech.simter.reactive.test.jpa.ReactiveDataJpaTest
import tech.simter.reactive.test.jpa.TestEntityManager

/**
 * Test [AttachmentDao.delete].
 *
 * @author RJ
 */
@SpringJUnitConfig(UnitTestConfiguration::class)
@ReactiveDataJpaTest
class DeleteMethodImplTest @Autowired constructor(
  val rem: TestEntityManager,
  val dao: AttachmentDao
) {
  @Test
  fun `delete multiple`() {
    // clean
    rem.executeUpdate { it.createQuery("delete from AttachmentPo") }

    // prepare data
    //            po100
    //       /            \
    //    po110          po120
    //   /     \      /    |     \
    // po111 po112  po121 po122 po123
    val basicPo = randomAttachmentPo()
    val po100 = basicPo.copy(id = "100", upperId = null, path = "path100")
    val po110 = basicPo.copy(id = "110", upperId = "100", path = "path110")
    val po120 = basicPo.copy(id = "120", upperId = "100", path = "path120")
    val po111 = basicPo.copy(id = "111", upperId = "110", path = "path111.xml")
    val po112 = basicPo.copy(id = "112", upperId = "110", path = "path112.xml")
    val po121 = basicPo.copy(id = "121", upperId = "120", path = "path121.xml")
    val po122 = basicPo.copy(id = "122", upperId = "120", path = "path122.xml")
    val po123 = basicPo.copy(id = "123", upperId = "120", path = "path123.xml")
    rem.persist(po100, po110, po111, po112, po120, po121, po122, po123)

    // invoke and verify
    dao.delete("110", "121")
      .test()
      .expectNext("path100/path110")
      .expectNext("path100/path120/path121.xml").verifyComplete()
    val newPoList = rem.queryList { it.createQuery("select a from AttachmentPo a", AttachmentPo::class.java) }
    assertEquals(4, newPoList.size)
    assertEquals(listOf("100", "120", "122", "123"), newPoList.map { it.id })
  }

  @Test
  fun `delete nothing`() {
    dao.delete().test().verifyComplete()
  }

  @Test
  fun `delete not exists`() {
    dao.delete(randomAttachmentId()).test().verifyComplete()
  }
}