package tech.simter.file.impl.dao.jpa

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import reactor.kotlin.test.test
import tech.simter.file.core.FileDao
import tech.simter.file.core.ModuleMatcher.Companion.autoModuleMatcher
import tech.simter.file.impl.dao.jpa.po.FileStorePo
import tech.simter.file.test.TestHelper.randomFileStore
import tech.simter.file.test.TestHelper.randomModuleValue
import tech.simter.reactive.test.jpa.ReactiveDataJpaTest
import tech.simter.reactive.test.jpa.TestEntityManager
import tech.simter.util.AssertUtils.assertSamePropertyHasSameValue
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
import java.util.*

/**
 * Test [FileDaoImpl.findList].
 *
 * @author RJ
 */
@SpringJUnitConfig(UnitTestConfiguration::class)
@ReactiveDataJpaTest
class FindListMethodImplTest @Autowired constructor(
  val rem: TestEntityManager,
  val dao: FileDao
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
    val ts = OffsetDateTime.now().truncatedTo(ChronoUnit.SECONDS)
    val files = listOf(
      FileStorePo.from(randomFileStore(module = module1, name = "abc", ts = ts.minusSeconds(9))),
      FileStorePo.from(randomFileStore(module = module1, name = "def", ts = ts.minusSeconds(8))),
      FileStorePo.from(randomFileStore(module = module2, name = "abc", ts = ts.minusSeconds(7)))
    )
    rem.persist(*files.toTypedArray())

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