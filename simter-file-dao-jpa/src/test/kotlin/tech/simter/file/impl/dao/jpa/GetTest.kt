package tech.simter.file.impl.dao.jpa

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import reactor.kotlin.test.test
import tech.simter.file.core.FileDao
import tech.simter.file.impl.dao.jpa.po.FileStorePo
import tech.simter.file.test.TestHelper.randomFileId
import tech.simter.file.test.TestHelper.randomFileStore
import tech.simter.reactive.test.jpa.ReactiveDataJpaTest
import tech.simter.reactive.test.jpa.TestEntityManager
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit.SECONDS

/**
 * Test [FileDaoImpl.get].
 *
 * @author RJ
 */
@SpringJUnitConfig(UnitTestConfiguration::class)
@ReactiveDataJpaTest
class GetTest @Autowired constructor(
  val rem: TestEntityManager,
  val dao: FileDao
) {
  @Test
  fun `get nothing`() {
    dao.get(randomFileId()).test().verifyComplete()
  }

  @Test
  fun `get it`() {
    // prepare data
    val file = FileStorePo.from(randomFileStore(ts = OffsetDateTime.now().truncatedTo(SECONDS)))
    rem.persist(file)

    // verify exists
    dao.get(file.id)
      .test()
      .expectNext(file)
      .verifyComplete()
  }
}