package tech.simter.file.rest.webflux.handler

import io.mockk.every
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import reactor.core.publisher.Mono
import tech.simter.file.core.FileService
import tech.simter.file.core.ModuleMatcher.ModuleEquals
import tech.simter.file.rest.webflux.UnitTestConfiguration
import tech.simter.util.RandomUtils.randomString
import java.net.URLEncoder

/**
 * Test delete file.
 *
 * `DELETE /file/$id?module`
 *
 * @author nb
 */
@SpringJUnitConfig(UnitTestConfiguration::class)
@WebFluxTest
class DeleteTest @Autowired constructor(
  @Value("\${simter-file.rest-context-path}")
  private val contextPath: String,
  private val client: WebTestClient,
  private val service: FileService
) {
  @Test
  fun `delete by id`() {
    val ids = (0..2).map { randomString() }
    every { service.delete(*ids.toTypedArray()) } returns Mono.just(2)

    client.delete().uri("$contextPath/${ids.joinToString(",")}").exchange()
      .expectStatus().isOk
      .expectBody<String>().isEqualTo("2")
  }

  @Test
  fun `delete by module`() {
    val module = "default"
    val encodeModule = URLEncoder.encode(module, "UTF-8")
    val moduleMatcher = ModuleEquals(module)
    every { service.delete(moduleMatcher) } returns Mono.just(1)

    client.delete().uri("$contextPath/${encodeModule}/?module").exchange()
      .expectStatus().isOk
      .expectBody<String>().isEqualTo("1")
  }
}