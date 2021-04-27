package tech.simter.file.rest.webflux.handler

import io.mockk.every
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import reactor.core.publisher.Flux
import tech.simter.file.core.FileService
import tech.simter.file.core.ModuleMatcher.ModuleEquals
import tech.simter.file.rest.webflux.UnitTestConfiguration
import tech.simter.file.test.TestHelper.randomFileStore
import tech.simter.file.test.TestHelper.randomModuleValue

/**
 * Test find files none-pageable.
 *
 * @author RJ
 */
@SpringJUnitConfig(UnitTestConfiguration::class)
@WebFluxTest
class FindListTest @Autowired constructor(
  private val json: Json,
  private val client: WebTestClient,
  private val service: FileService
) {
  @Test
  fun found() {
    // prepare data
    val module = randomModuleValue()
    val moduleMatcher = ModuleEquals(module)
    val f1 = randomFileStore(module = module)
    val f2 = randomFileStore(module = module)

    // mock
    every { service.findList(moduleMatcher) } returns Flux.just(f2, f1)

    // find it
    client.get().uri("/?module=$module")
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(APPLICATION_JSON)
      .expectBody<String>().isEqualTo(json.encodeToString(listOf(f2, f1)))
  }

  @Test
  fun notFound() {
    // prepare data
    val module = randomModuleValue()
    val moduleMatcher = ModuleEquals(module)

    // mock
    every { service.findList(moduleMatcher) } returns Flux.empty()

    client.get().uri("/?module=$module")
      .exchange()
      .expectStatus().isOk
      .expectBody<String>().isEqualTo("[]")
  }
}