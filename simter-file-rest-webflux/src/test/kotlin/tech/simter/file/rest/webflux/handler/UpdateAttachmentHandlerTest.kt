package tech.simter.file.rest.webflux.handler

import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType.APPLICATION_JSON_UTF8
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import org.springframework.test.web.reactive.server.WebTestClient.bindToRouterFunction
import org.springframework.web.reactive.config.EnableWebFlux
import org.springframework.web.reactive.function.server.RouterFunctions.route
import reactor.core.publisher.Mono
import tech.simter.exception.ForbiddenException
import tech.simter.exception.NotFoundException
import tech.simter.exception.PermissionDeniedException
import tech.simter.file.dto.AttachmentDto4Update
import tech.simter.file.rest.webflux.Utils.randomString
import tech.simter.file.rest.webflux.handler.UpdateAttachmentHandler.Companion.REQUEST_PREDICATE
import tech.simter.file.service.AttachmentService
import java.util.*

/**
 * Test [UpdateAttachmentHandler].
 *
 * @author zh
 */
@SpringJUnitConfig(UpdateAttachmentHandler::class)
@EnableWebFlux
@MockBean(AttachmentService::class)
internal class UpdateAttachmentHandlerTest @Autowired constructor(
  private val service: AttachmentService,
  handler: UpdateAttachmentHandler
) {
  private val client = bindToRouterFunction(route(REQUEST_PREDICATE, handler)).build()
  private val id = UUID.randomUUID().toString()
  private val url = "/attachment/$id"

  private fun randomAttachmentDto4Update(): AttachmentDto4Update {
    return AttachmentDto4Update().apply {
      name = randomString("name")
      upperId = UUID.randomUUID().toString()
      puid = randomString("puid")
    }
  }

  @Test
  fun `Success`() {
    val dto = randomAttachmentDto4Update()
    `when`(service.update(id, dto)).thenReturn(Mono.empty())

    // invoke request
    client.patch().uri(url)
      .contentType(APPLICATION_JSON_UTF8)
      .syncBody(dto.data)
      .exchange()
      .expectStatus().isNoContent

    // verify
    verify(service).update(id, dto)
  }

  @Test
  fun `Found nothing`() {
    // mock
    val dto = randomAttachmentDto4Update()
    `when`(service.update(id, dto)).thenReturn(Mono.error(NotFoundException("")))

    // invoke request
    client.patch().uri(url)
      .contentType(APPLICATION_JSON_UTF8)
      .syncBody(dto.data)
      .exchange()
      .expectStatus().isNotFound

    // verify
    verify(service).update(id, dto)
  }

  @Test
  fun `Failed by permission denied`() {
    // mock
    val dto = randomAttachmentDto4Update()
    `when`(service.update(id, dto)).thenReturn(Mono.error(PermissionDeniedException("")))

    // invoke request
    client.patch().uri(url)
      .contentType(APPLICATION_JSON_UTF8)
      .syncBody(dto.data)
      .exchange()
      .expectStatus().isForbidden

    // verify
    verify(service).update(id, dto)
  }

  @Test
  fun `Failed by across module`() {
    // mock
    val dto = randomAttachmentDto4Update()
    `when`(service.update(id, dto)).thenReturn(Mono.error(ForbiddenException("")))

    // invoke request
    client.patch().uri(url)
      .contentType(APPLICATION_JSON_UTF8)
      .syncBody(dto.data)
      .exchange()
      .expectStatus().isForbidden

    // verify
    verify(service).update(id, dto)
  }
}