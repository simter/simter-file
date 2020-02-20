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
import tech.simter.exception.PermissionDeniedException
import tech.simter.file.core.AttachmentService
import tech.simter.file.impl.domain.AttachmentImpl
import tech.simter.file.rest.webflux.UnitTestConfiguration
import java.time.OffsetDateTime
import java.util.*

/**
 * Test [AttachmentFormHandler].
 *
 * @author JF
 * @author RJ
 * @author zh
 */
@SpringJUnitConfig(UnitTestConfiguration::class)
@WebFluxTest
class AttachmentFormHandlerTest @Autowired constructor(
  private val client: WebTestClient,
  private val service: AttachmentService
) {
  @Test
  fun found() {
    // mock
    val id = UUID.randomUUID().toString()
    val now = OffsetDateTime.now()
    val attachment = AttachmentImpl(id, "/path", "name", "type", 100,
      now, "Simter", now, "Simter", "0")
    every { service.get(id) } returns Mono.just(attachment)

    // invoke
    client.get().uri("/attachment/$id")
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(APPLICATION_JSON)
      .expectBody().jsonPath("$.id").isEqualTo(id)

    // verify
    verify { service.get(id) }
  }

  @Test
  fun notFound() {
    // mock
    val id = UUID.randomUUID().toString()
    every { service.get(id) } returns Mono.empty()

    // invoke
    client.get().uri("/attachment/$id").exchange().expectStatus().isNotFound

    // verify
    verify { service.get(id) }
  }

  @Test
  fun failedByPermissionDenied() {
    // mock
    val id = UUID.randomUUID().toString()
    every { service.get(id) } returns Mono.error(PermissionDeniedException())

    // invoke
    client.get().uri("/attachment/$id").exchange().expectStatus().isForbidden

    // verify
    verify { service.get(id) }
  }
}