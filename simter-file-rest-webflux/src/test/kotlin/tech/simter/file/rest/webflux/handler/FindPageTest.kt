package tech.simter.file.rest.webflux.handler

import io.mockk.every
import io.mockk.verify
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import reactor.core.publisher.Mono
import tech.simter.file.core.FileService
import tech.simter.file.core.FileStore
import tech.simter.file.core.ModuleMatcher.ModuleEquals
import tech.simter.file.rest.webflux.UnitTestConfiguration
import tech.simter.file.test.TestHelper.randomFileStore
import tech.simter.file.test.TestHelper.randomModuleValue
import tech.simter.kotlin.data.Page
import tech.simter.kotlin.data.Page.Companion.MappedType.OffsetLimit
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
  private val json: Json,
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
    val page = Page.of(rows = rows, total = total, limit = limit, offset = 0)
    val pageJsonString = json.encodeToString(Page.toMap(page, json, OffsetLimit))
    every {
      service.findPage(moduleMatcher = moduleMatcher, limit = Optional.of(limit))
    } returns Mono.just(page)

    // find it
    client.get().uri("/?pageable&module=$module&limit=$limit")
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(APPLICATION_JSON)
      .expectBody<String>().isEqualTo(pageJsonString)
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
    val emptyPage = Page.of(
      rows = emptyList<FileStore>(),
      total = 0,
      offset = 0,
      limit = 25
    )
    every { service.findPage(moduleMatcher) } returns Mono.just(emptyPage)
    val pageJsonString = json.encodeToString(Page.toMap(emptyPage, json, OffsetLimit))

    client.get().uri("/?pageable&module=$module")
      .exchange()
      .expectStatus().isOk
      .expectBody<String>().isEqualTo(pageJsonString)
    verify(exactly = 1) { service.findPage(moduleMatcher) }
  }
}