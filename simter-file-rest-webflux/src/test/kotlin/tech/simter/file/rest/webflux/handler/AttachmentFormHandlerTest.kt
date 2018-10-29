package tech.simter.file.rest.webflux.handler

import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import org.springframework.test.web.reactive.server.WebTestClient.bindToRouterFunction
import org.springframework.web.reactive.config.EnableWebFlux
import org.springframework.web.reactive.function.server.RouterFunctions.route
import reactor.core.publisher.Mono
import tech.simter.file.po.Attachment
import tech.simter.file.rest.webflux.handler.AttachmentFormHandler.Companion.REQUEST_PREDICATE
import tech.simter.file.service.AttachmentService
import java.time.OffsetDateTime
import java.util.*

/**
 * Test AttachmentFormHandler.
 *
 * @author JF
 * @author RJ
 */
@SpringJUnitConfig(AttachmentFormHandler::class)
@EnableWebFlux
@MockBean(AttachmentService::class)
internal class AttachmentFormHandlerTest @Autowired constructor(
  private val service: AttachmentService,
  handler: AttachmentFormHandler
) {
  private val client = bindToRouterFunction(route(REQUEST_PREDICATE, handler)).build()

  @Test
  fun found() {
    // mock
    val id = UUID.randomUUID().toString()
    val now = OffsetDateTime.now()
    val attachment = Attachment(id, "/path", "name", "type", 100,
      now, "Simter", now, "Simter", "0")
    `when`(service.get(id)).thenReturn(Mono.just(attachment))

    // invoke
    client.get().uri("/attachment/$id")
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON_UTF8)
      .expectBody().jsonPath("$.id").isEqualTo(id)

    // verify
    verify(service).get(id)
  }

  @Test
  fun notFound() {
    // mock
    val id = UUID.randomUUID().toString()
    `when`(service.get(id)).thenReturn(Mono.empty())

    // invoke
    client.get().uri("/attachment/$id").exchange().expectStatus().isNotFound

    // verify
    verify(service).get(id)
  }
}