package tech.simter.file.rest.webflux.handler

import io.mockk.every
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.http.MediaType.TEXT_PLAIN
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import reactor.core.publisher.Mono
import tech.simter.exception.PermissionDeniedException
import tech.simter.file.core.FileService
import tech.simter.file.core.ModuleMatcher.ModuleEquals
import tech.simter.file.rest.webflux.UnitTestConfiguration
import tech.simter.util.RandomUtils.randomString

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
    val moduleMatcher = ModuleEquals(module)
    every { service.delete(moduleMatcher) } returns Mono.just(1)

    client.delete().uri("$contextPath/{module}/?module", module).exchange()
      .expectStatus().isOk
      .expectBody<String>().isEqualTo("1")
  }

  @Test
  fun `failed by permission denied`() {
    // mock
    val id = randomString()
    val msg = randomString()
    every { service.delete(id) } returns Mono.error(PermissionDeniedException(msg))

    // invoke and verify
    client.delete().uri("$contextPath/$id").exchange()
      .expectStatus().isForbidden
      .expectHeader().contentTypeCompatibleWith(TEXT_PLAIN)
      .expectBody<String>().isEqualTo(msg)
    verify(exactly = 1) { service.delete(id) }
  }
}