package tech.simter.file.impl.dao.r2dbc

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import reactor.kotlin.test.test
import tech.simter.file.core.FileDao
import tech.simter.file.core.ModuleMatcher.Companion.autoModuleMatcher
import tech.simter.file.impl.dao.r2dbc.TestHelper.insert
import tech.simter.file.test.TestHelper.randomFileStore
import tech.simter.file.test.TestHelper.randomModuleValue
import tech.simter.util.AssertUtils.assertSamePropertyHasSameValue
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit.SECONDS
import java.util.*

/**
 * Test [FileDaoImpl.findList].
 *
 * @author RJ
 */
@SpringJUnitConfig(UnitTestConfiguration::class)
@DataR2dbcTest
class FindListMethodImplTest @Autowired constructor(
  private val client: DatabaseClient,
  private val dao: FileDao
) {
  @Test
  fun `found nothing`() {
    dao.findList(moduleMatcher = autoModuleMatcher(randomModuleValue()))
      .test()
      .verifyComplete()
  }

  @Test
  fun `found something`() {
    // prepare data
    val module1 = randomModuleValue()
    val module2 = randomModuleValue()
    val ts = OffsetDateTime.now().truncatedTo(SECONDS)
    val files = listOf(
      insert(client = client, file = randomFileStore(module = module1, name = "abc", ts = ts.minusSeconds(9))),
      insert(client = client, file = randomFileStore(module = module1, name = "def", ts = ts.minusSeconds(8))),
      insert(client = client, file = randomFileStore(module = module2, name = "abc", ts = ts.minusSeconds(7)))
    )

    // 1. find all module1 without fuzzy search
    dao.findList(moduleMatcher = autoModuleMatcher(module1))
      .collectList()
      .test()
      .assertNext { actual ->
        assertThat(actual).hasSize(2)
        assertSamePropertyHasSameValue(files[1], actual[0])
        assertSamePropertyHasSameValue(files[0], actual[1])
      }
      .verifyComplete()

    // 2. find all module1 with fuzzy search
    dao.findList(moduleMatcher = autoModuleMatcher(module1), search = Optional.of("a"))
      .collectList()
      .test()
      .assertNext { actual ->
        assertThat(actual).hasSize(1)
        assertSamePropertyHasSameValue(files[0], actual[0])
      }
      .verifyComplete()
  }
}