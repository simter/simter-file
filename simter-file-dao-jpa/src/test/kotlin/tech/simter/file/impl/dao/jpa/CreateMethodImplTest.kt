package tech.simter.file.impl.dao.jpa

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import reactor.kotlin.test.test
import tech.simter.file.core.FileDao
import tech.simter.file.impl.dao.jpa.po.FileStorePo
import tech.simter.file.test.TestHelper.randomFileStore
import tech.simter.reactive.test.jpa.ReactiveDataJpaTest
import tech.simter.reactive.test.jpa.TestEntityManager
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit.SECONDS

/**
 * Test [FileDao.create].
 *
 * @author RJ
 */
@SpringJUnitConfig(UnitTestConfiguration::class)
@ReactiveDataJpaTest
class CreateMethodImplTest @Autowired constructor(
  val rem: TestEntityManager,
  val dao: FileDao
) {
  @Test
  fun test() {
    // invoke
    val file = randomFileStore(ts = OffsetDateTime.now().truncatedTo(SECONDS))
    dao.create(file).test().expectNext(file.id).verifyComplete()

    // verify
    assertThat(rem.find(FileStorePo::class.java, file.id).get())
      .isEqualToComparingFieldByField(file)
  }
}