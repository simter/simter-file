package tech.simter.file.rest.webflux.handler

import io.mockk.every
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.core.publisher.Mono
import tech.simter.file.core.FileService
import tech.simter.file.core.ModuleMatcher.ModuleEquals
import tech.simter.file.rest.webflux.UnitTestConfiguration
import tech.simter.file.test.TestHelper.randomFileStore
import tech.simter.file.test.TestHelper.randomModuleValue
import tech.simter.kotlin.data.Page
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit.SECONDS
import java.util.*

/**
 * Test find files pageable.
 *
 * @author RJ
 */
@SpringJUnitConfig(UnitTestConfiguration::class)
@WebFluxTest
class FindPageTest @Autowired constructor(
  private val client: WebTestClient,
  private val service: FileService
) {
  @Test
  fun found() {
    // prepare data
    val module = randomModuleValue()
    val moduleMatcher = ModuleEquals(module)
    val ts = OffsetDateTime.now().truncatedTo(SECONDS)
    val rows = listOf(
      randomFileStore(module = module, ts = ts),
      randomFileStore(module = module, ts = ts.minusSeconds(1))
    )

    // mock
    val limit = 20
    val total = rows.size.toLong()
    every {
      service.findPage(moduleMatcher = moduleMatcher, limit = Optional.of(limit))
    } returns Mono.just(Page.of(rows = rows, total = total, limit = limit, offset = 0))

    // find it
    client.get().uri("/?pageable&module=$module&limit=$limit")
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(APPLICATION_JSON)
      .expectBody()
      .jsonPath("$.offset").isEqualTo(0)
      .jsonPath("$.limit").isEqualTo(limit)
      .jsonPath("$.total").isEqualTo(total)
      .jsonPath("$.rows.size()").isEqualTo(rows.size)

      .jsonPath("$.rows[0].id").isEqualTo(rows[0].id)
      .jsonPath("$.rows[0].module").isEqualTo(rows[0].module)
      .jsonPath("$.rows[0].name").isEqualTo(rows[0].name)
      .jsonPath("$.rows[0].type").isEqualTo(rows[0].type)
      .jsonPath("$.rows[0].path").isEqualTo(rows[0].path)
      .jsonPath("$.rows[0].size").isEqualTo(rows[0].size)
      .jsonPath("$.rows[0].creator").isEqualTo(rows[0].creator)
      .jsonPath("$.rows[0].modifier").isEqualTo(rows[0].modifier)
      .jsonPath("$.rows[0].createOn").isEqualTo(rows[0].createOn.toString())
      .jsonPath("$.rows[0].modifyOn").isEqualTo(rows[0].modifyOn.toString())

      .jsonPath("$.rows[1].id").isEqualTo(rows[1].id)
      .jsonPath("$.rows[1].module").isEqualTo(rows[1].module)
      .jsonPath("$.rows[1].name").isEqualTo(rows[1].name)
      .jsonPath("$.rows[1].type").isEqualTo(rows[1].type)
      .jsonPath("$.rows[1].path").isEqualTo(rows[1].path)
      .jsonPath("$.rows[1].size").isEqualTo(rows[1].size)
      .jsonPath("$.rows[1].creator").isEqualTo(rows[1].creator)
      .jsonPath("$.rows[1].modifier").isEqualTo(rows[1].modifier)
      .jsonPath("$.rows[1].createOn").isEqualTo(rows[1].createOn.toString())
      .jsonPath("$.rows[1].modifyOn").isEqualTo(rows[1].modifyOn.toString())
    verify(exactly = 1) {
      service.findPage(moduleMatcher = moduleMatcher, limit = Optional.of(limit))
    }
  }

  @Test
  fun notFound() {
    // prepare data
    val module = randomModuleValue()
    val moduleMatcher = ModuleEquals(module)

    // mock
    every { service.findPage(moduleMatcher) } returns Mono.just(Page.of(
      rows = emptyList(),
      total = 0,
      offset = 0,
      limit = 25
    ))

    client.get().uri("/?pageable&module=$module")
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.offset").isEqualTo(0)
      .jsonPath("$.limit").isEqualTo(25)
      .jsonPath("$.total").isEqualTo(0)
      .jsonPath("$.rows.size()").isEqualTo(0)
    verify(exactly = 1) { service.findPage(moduleMatcher) }
  }
}