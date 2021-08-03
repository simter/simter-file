package tech.simter.file.impl.dao.r2dbc

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import reactor.kotlin.test.test
import tech.simter.file.core.FileDao
import tech.simter.file.core.FileUpdate
import tech.simter.file.impl.dao.r2dbc.TestHelper.insert
import java.util.*

/**
 * Test [FileDaoImpl.update]
 *
 * @author nb
 */
@SpringJUnitConfig(UnitTestConfiguration::class)
@DataR2dbcTest
class UpdateMethodImplTest @Autowired constructor(
  private val client: DatabaseClient,
  private val dao: FileDao
) {
  @Test
  fun test() {
    // prepare data
    val file = insert(client = client)
    val updateInfo = FileUpdate.Impl(module = Optional.of("test"))

    // verify and invoke
    dao.update(file.id, updateInfo).test().expectNext(true).verifyComplete()
    dao.get(file.id).test().assertNext { assertThat(it.module).isEqualTo("test") }.verifyComplete()
  }
}