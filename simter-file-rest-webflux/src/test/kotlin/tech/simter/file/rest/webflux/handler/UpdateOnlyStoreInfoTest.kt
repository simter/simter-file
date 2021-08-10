package tech.simter.file.rest.webflux.handler

import io.mockk.every
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.core.publisher.Mono
import tech.simter.file.core.FileService
import tech.simter.file.core.FileUpdateDescriber
import tech.simter.file.rest.webflux.UnitTestConfiguration
import tech.simter.util.RandomUtils.randomString
import java.util.*

/**
 * Test update only file store info.
 *
 * @author nb
 */
@SpringJUnitConfig(UnitTestConfiguration::class)
@WebFluxTest
class UpdateOnlyStoreInfoTest @Autowired constructor(
  @Value("\${simter-file.rest-context-path}")
  private val contextPath: String,
  private val client: WebTestClient,
  private val service: FileService
) {
  @Test
  fun success() {
    // prepare data
    val id = randomString()
    val module = randomString()
    val fileUpdateDescriber = FileUpdateDescriber.Impl(module = Optional.of(module))

    // mock
    every { service.update(id, fileUpdateDescriber) } returns Mono.empty<Void>()

    // invoke request
    client.patch()
      .uri { it.path("$contextPath/${id}").queryParam("module", module).build() }
      .exchange()
      .expectStatus().isNoContent

    // verify
    verify { service.update(id, fileUpdateDescriber) }
  }
}