package tech.simter.file.rest.webflux.handler

import io.mockk.every
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import org.springframework.test.web.reactive.server.WebTestClient
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
      .expectBody()
      .jsonPath("$.size()").isEqualTo(2)

      .jsonPath("$[0].id").isEqualTo(f2.id)
      .jsonPath("$[0].module").isEqualTo(f2.module)
      .jsonPath("$[0].name").isEqualTo(f2.name)
      .jsonPath("$[0].type").isEqualTo(f2.type)
      .jsonPath("$[0].size").isEqualTo(f2.size)

      .jsonPath("$[1].id").isEqualTo(f1.id)
      .jsonPath("$[1].module").isEqualTo(f1.module)
      .jsonPath("$[1].name").isEqualTo(f1.name)
      .jsonPath("$[1].type").isEqualTo(f1.type)
      .jsonPath("$[1].size").isEqualTo(f1.size)
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
      .expectBody()
      .json("[]")
  }
}