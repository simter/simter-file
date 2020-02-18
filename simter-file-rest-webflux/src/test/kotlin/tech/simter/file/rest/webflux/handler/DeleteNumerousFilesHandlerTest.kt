package tech.simter.file.rest.webflux.handler

import io.mockk.every
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.http.HttpMethod.DELETE
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.core.publisher.Mono
import tech.simter.exception.ForbiddenException
import tech.simter.exception.PermissionDeniedException
import tech.simter.file.core.AttachmentService
import tech.simter.file.rest.webflux.UnitTestConfiguration
import java.util.*

/**
 * Test [DeleteNumerousFilesHandler].
 *
 * @author zh
 * @author RJ
 */
@SpringJUnitConfig(UnitTestConfiguration::class)
@WebFluxTest
@TestPropertySource(properties = ["simter.file.root=target/files"])
class DeleteNumerousFilesHandlerTest @Autowired constructor(
  private val client: WebTestClient,
  private val service: AttachmentService
) {
  @Test
  fun delete() {
    // mock
    val ids = Array(4) { UUID.randomUUID().toString() }
    every { service.delete(*ids) } returns Mono.empty()

    // invoke
    client.method(DELETE).uri("/")
      .contentType(APPLICATION_JSON)
      .bodyValue(ids)
      .exchange().expectStatus().isNoContent

    // verify
    verify { service.delete(*ids) }
  }

  @Test
  fun failedByPermissionDenied() {
    // mock
    val ids = Array(4) { UUID.randomUUID().toString() }
    every { service.delete(*ids) } returns Mono.error(PermissionDeniedException())

    // invoke
    client.method(DELETE).uri("/")
      .contentType(APPLICATION_JSON)
      .bodyValue(ids)
      .exchange()
      .expectStatus().isForbidden

    // verify
    verify { service.delete(*ids) }
  }

  @Test
  fun failedByAcrossModule() {
    // mock
    val ids = Array(4) { UUID.randomUUID().toString() }
    every { service.delete(*ids) } returns Mono.error(ForbiddenException())

    // invoke
    client.method(DELETE).uri("/")
      .contentType(APPLICATION_JSON)
      .bodyValue(ids)
      .exchange()
      .expectStatus().isForbidden

    // verify
    verify { service.delete(*ids) }
  }
}