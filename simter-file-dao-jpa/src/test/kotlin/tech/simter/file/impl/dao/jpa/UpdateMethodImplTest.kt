package tech.simter.file.impl.dao.jpa

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import reactor.kotlin.test.test
import tech.simter.file.core.FileDao
import tech.simter.file.core.FileUpdateDescriber
import tech.simter.file.test.TestHelper.randomFileStore
import tech.simter.reactive.test.jpa.ReactiveDataJpaTest
import tech.simter.reactive.test.jpa.TestEntityManager
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
import java.util.*

/**
 * Test [FileDao.update].
 *
 * @author nb
 */
@SpringJUnitConfig(UnitTestConfiguration::class)
@ReactiveDataJpaTest
class UpdateMethodImplTest @Autowired constructor(
  val rem: TestEntityManager,
  val dao: FileDao
) {
  @Test
  fun test() {
    // prepare data
    val file = randomFileStore(ts = OffsetDateTime.now().truncatedTo(ChronoUnit.SECONDS))
    val id = dao.create(file).block()!!
    val updateInfo = FileUpdateDescriber.Impl(module = Optional.of("test"))

    // invoke and verify
    dao.update(id, updateInfo).test().expectNext(true).verifyComplete()
    dao.findById(id).test().assertNext { assertThat(it.module).isEqualTo("test") }.verifyComplete()
  }
}