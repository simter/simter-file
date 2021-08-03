package tech.simter.file.impl.dao.jpa

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import reactor.kotlin.test.test
import tech.simter.file.core.FileDao
import tech.simter.file.core.ModuleMatcher.Companion.autoModuleMatcher
import tech.simter.file.test.TestHelper.randomFileStore
import tech.simter.reactive.test.jpa.ReactiveDataJpaTest
import tech.simter.reactive.test.jpa.TestEntityManager
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit

/**
 * Test [FileDao.delete].
 *
 * @author nb
 */
@SpringJUnitConfig(UnitTestConfiguration::class)
@ReactiveDataJpaTest
class DeleteTest @Autowired constructor(
  val rem: TestEntityManager,
  val dao: FileDao
) {
  private val file1 = randomFileStore(ts = OffsetDateTime.now().truncatedTo(ChronoUnit.SECONDS), module = "/a/b/")
  private val file2 = randomFileStore(ts = OffsetDateTime.now().truncatedTo(ChronoUnit.SECONDS), module = "/a/c/")

  @BeforeEach
  fun init() {
    dao.create(file1).test().expectNext(file1.id).verifyComplete()
    dao.create(file2).test().expectNext(file2.id).verifyComplete()
  }

  @Test
  fun `delete by id`() {
    dao.delete(file1.id, file2.id).test().expectNext(2).verifyComplete()
  }

  @Test
  fun `delete by module`() {
    dao.delete(autoModuleMatcher("/a/%")).test().expectNext(2).verifyComplete()
  }
}