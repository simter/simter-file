package tech.simter.file.impl.dao.r2dbc

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.r2dbc.core.DatabaseClient
import reactor.kotlin.test.test
import tech.simter.file.core.FileDao
import tech.simter.file.core.ModuleMatcher.Companion.autoModuleMatcher
import tech.simter.file.impl.dao.r2dbc.TestHelper.insert
import tech.simter.file.test.TestHelper.randomFileStore
import tech.simter.file.test.TestHelper.randomModuleValue
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
import java.util.*

/**
 * Test [FileDaoImpl.findPage].
 *
 * @author RJ
 */
@SpringBootTest(classes = [UnitTestConfiguration::class])
class FindPageMethodImplTest @Autowired constructor(
  private val client: DatabaseClient,
  private val dao: FileDao
) {
  @Test
  fun `found nothing`() {
    val limit = 20
    dao.findPage(moduleMatcher = autoModuleMatcher(randomModuleValue()), limit = limit)
      .test()
      .assertNext { page ->
        assertThat(page.offset).isEqualTo(0)
        assertThat(page.limit).isEqualTo(limit)
        assertThat(page.total).isEqualTo(0)
        assertThat(page.rows).isEmpty()
      }
      .verifyComplete()
  }

  @Test
  fun `found something`() {
    // prepare data
    val module1 = randomModuleValue()
    val module2 = randomModuleValue()
    val ts = OffsetDateTime.now().truncatedTo(ChronoUnit.SECONDS)
    val files = listOf(
      insert(client = client, file = randomFileStore(module = module1, name = "abc", ts = ts.minusSeconds(9))),
      insert(client = client, file = randomFileStore(module = module1, name = "def", ts = ts.minusSeconds(8))),
      insert(client = client, file = randomFileStore(module = module1, name = "gha", ts = ts.minusSeconds(7))),
      insert(client = client, file = randomFileStore(module = module2, name = "abc", ts = ts.minusSeconds(6)))
    )

    // 1. find all module1 without fuzzy search
    val limit = 2
    dao.findPage(
        moduleMatcher = autoModuleMatcher(module1),
        offset = 0,
        limit = limit
      )
      .test()
      .assertNext { page ->
        assertThat(page.offset).isEqualTo(0)
        assertThat(page.limit).isEqualTo(limit)
        assertThat(page.total).isEqualTo(3)
        assertThat(page.rows).hasSize(2)
        assertThat(page.rows[0]).isEqualTo(files[2])
        assertThat(page.rows[1]).isEqualTo(files[1])
      }
      .verifyComplete()

    // 2. find all module1 with fuzzy search
    dao.findPage(
        moduleMatcher = autoModuleMatcher(module1),
        offset = 0,
        limit = limit,
        search = Optional.of("a")
      )
      .test()
      .assertNext { page ->
        assertThat(page.offset).isEqualTo(0)
        assertThat(page.limit).isEqualTo(limit)
        assertThat(page.total).isEqualTo(2)
        assertThat(page.rows).hasSize(2)
        assertThat(page.rows[0]).isEqualTo(files[2])
        assertThat(page.rows[1]).isEqualTo(files[0])
      }
      .verifyComplete()
  }
}