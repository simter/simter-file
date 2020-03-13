package tech.simter.file.impl.dao.r2dbc

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.r2dbc.core.DatabaseClient
import reactor.kotlin.test.test
import tech.simter.file.TABLE_FILE
import tech.simter.file.core.FileDao
import tech.simter.file.test.TestHelper.randomFileStore

/**
 * Test [FileDaoImpl.create].
 *
 * @author RJ
 */
@SpringBootTest(classes = [UnitTestConfiguration::class])
class CreateMethodImplTest @Autowired constructor(
  private val client: DatabaseClient,
  private val dao: FileDao
) {
  @Test
  fun test() {
    // invoke
    val file = randomFileStore()
    dao.create(file).test().expectNext(file.id).verifyComplete()

    // verify
    client.execute("select count(*) from $TABLE_FILE where id = :id")
      .bind("id", file.id)
      .map { row -> row[0] as Long }
      .one()
      .test()
      .expectNext(1L)
      .verifyComplete()
  }
}