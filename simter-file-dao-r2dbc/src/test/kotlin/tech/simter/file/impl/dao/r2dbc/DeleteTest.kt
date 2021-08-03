package tech.simter.file.impl.dao.r2dbc

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import reactor.kotlin.test.test
import tech.simter.file.core.FileDao
import tech.simter.file.core.ModuleMatcher
import tech.simter.file.test.TestHelper.randomFileStore

/**
 * Test [FileDaoImpl.delete].
 *
 * @author nb
 */
@SpringJUnitConfig(UnitTestConfiguration::class)
@DataR2dbcTest
class DeleteTest @Autowired constructor(
  private val dao: FileDao
) {
  private val file1 = randomFileStore(module = "/a/b/")
  private val file2 = randomFileStore(module = "/a/c/")

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
    dao.delete(ModuleMatcher.autoModuleMatcher("/a/%")).test().expectNext(2).verifyComplete()
  }
}