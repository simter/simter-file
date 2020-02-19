package tech.simter.file.impl.dao.mongo

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import reactor.kotlin.test.test
import tech.simter.file.core.AttachmentDao
import tech.simter.file.impl.dao.mongo.TestHelper.cleanDatabase
import tech.simter.file.impl.dao.mongo.TestHelper.randomAttachmentId
import tech.simter.file.impl.dao.mongo.TestHelper.randomAttachmentPo

/**
 * Test [AttachmentDaoImpl.delete].
 *
 * @author RJ
 */
@SpringJUnitConfig(UnitTestConfiguration::class)
@DataMongoTest
class DeleteMethodImplTest @Autowired constructor(
  private val dao: AttachmentDao,
  private val repository: AttachmentReactiveRepository
) {
  @Test
  fun `delete multiple`() {
    // clean
    cleanDatabase(repository)

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
    val poList = listOf(po100, po110, po111, po112, po120, po121, po122, po123)
    repository.saveAll(poList).test().expectNextCount(poList.size.toLong()).verifyComplete()

    // invoke and verify
    dao.delete("110", "121")
      .test()
      .expectNext("path100/path110")
      .expectNext("path100/path120/path121.xml").verifyComplete()
    repository.findAll().collectList()
      .test()
      .expectNext(listOf(po100, po120, po122, po123))
      .verifyComplete()
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