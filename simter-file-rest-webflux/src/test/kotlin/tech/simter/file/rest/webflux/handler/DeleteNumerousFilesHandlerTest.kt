package tech.simter.file.rest.webflux.handler

import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.HttpMethod.DELETE
import org.springframework.http.MediaType.APPLICATION_JSON_UTF8
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import org.springframework.test.web.reactive.server.WebTestClient.bindToRouterFunction
import org.springframework.web.reactive.config.EnableWebFlux
import org.springframework.web.reactive.function.server.RouterFunctions.route
import reactor.core.publisher.Mono
import tech.simter.exception.ForbiddenException
import tech.simter.exception.PermissionDeniedException
import tech.simter.file.rest.webflux.handler.DeleteNumerousFilesHandler.Companion.REQUEST_PREDICATE
import tech.simter.file.core.AttachmentService
import java.util.*

/**
 * Test [DeleteNumerousFilesHandler].
 *
 * @author zh
 */
@SpringJUnitConfig(DeleteNumerousFilesHandler::class)
@EnableWebFlux
@MockBean(AttachmentService::class)
@TestPropertySource(properties = ["simter.file.root=target/files"])
internal class DeleteNumerousFilesHandlerTest @Autowired constructor(
  private val service: AttachmentService,
  handler: DeleteNumerousFilesHandler
) {
  private val client = bindToRouterFunction(route(REQUEST_PREDICATE, handler)).build()

  @Test
  fun delete() {
    // mock
    val ids = Array(4) { UUID.randomUUID().toString() }
    `when`(service.delete(*ids)).thenReturn(Mono.empty())

    // invoke
    client.method(DELETE).uri("/")
      .contentType(APPLICATION_JSON_UTF8).syncBody(ids)
      .exchange().expectStatus().isNoContent

    // verify
    Mockito.verify(service).delete(*ids)
  }

  @Test
  fun failedByPermissionDenied() {
    // mock
    val ids = Array(4) { UUID.randomUUID().toString() }
    `when`(service.delete(*ids)).thenReturn(Mono.error(PermissionDeniedException()))

    // invoke
    client.delete().uri("/${ids.joinToString(",")}").exchange().expectStatus().isForbidden

    // verify
    Mockito.verify(service).delete(*ids)
  }

  @Test
  fun failedByAcrossModule() {
    // mock
    val ids = Array(4) { UUID.randomUUID().toString() }
    `when`(service.delete(*ids)).thenReturn(Mono.error(ForbiddenException()))

    // invoke
    client.delete().uri("/${ids.joinToString(",")}").exchange().expectStatus().isForbidden

    // verify
    Mockito.verify(service).delete(*ids)
  }
}