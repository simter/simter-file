package tech.simter.file.impl.dao.r2dbc

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import reactor.kotlin.test.test
import tech.simter.file.core.FileDao
import tech.simter.file.impl.dao.r2dbc.TestHelper.insert
import tech.simter.file.test.TestHelper.randomFileId

/**
 * Test [FileDaoImpl.get]
 *
 * @author RJ
 */
@SpringJUnitConfig(UnitTestConfiguration::class)
@DataR2dbcTest
class GetTest @Autowired constructor(
  private val client: DatabaseClient,
  private val dao: FileDao
) {
  @Test
  fun `get nothing`() {
    dao.get(randomFileId()).test().verifyComplete()
  }

  @Test
  fun `get it`() {
    // prepare data
    val file = insert(client = client)

    // verify exists
    dao.get(file.id)
      .test()
      .assertNext {
        assertThat(it).usingRecursiveComparison().isEqualTo(file)
      }
      .verifyComplete()
  }
}