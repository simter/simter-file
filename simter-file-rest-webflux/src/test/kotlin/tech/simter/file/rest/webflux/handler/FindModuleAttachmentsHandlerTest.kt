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
import reactor.core.publisher.Flux
import tech.simter.exception.PermissionDeniedException
import tech.simter.file.po.Attachment
import tech.simter.file.rest.webflux.handler.FindModuleAttachmentsHandler.Companion.REQUEST_PREDICATE
import tech.simter.file.service.AttachmentService
import java.time.OffsetDateTime

/**
 * Test [FindModuleAttachmentsHandler].
 *
 * @author JW
 * @author zh
 */
@SpringJUnitConfig(FindModuleAttachmentsHandler::class)
@EnableWebFlux
@MockBean(AttachmentService::class)
internal class FindModuleAttachmentsHandlerTest @Autowired constructor(
  private val service: AttachmentService,
  handler: FindModuleAttachmentsHandler
) {
  private val client = bindToRouterFunction(route(REQUEST_PREDICATE, handler)).build()

  @Test
  fun file() {
    // mock
    val puid = "puid1"
    val upperId = "1"
    val now = OffsetDateTime.now()
    `when`<Flux<Attachment>>(service.find(puid, upperId)).thenReturn(Flux.just(
      Attachment(
        id = "1",
        path = "path/",
        name = "name",
        type = "png",
        size = 100L,
        createOn = now,
        creator = "creator",
        modifyOn = now,
        modifier = "creator",
        puid = puid,
        upperId = upperId))
    )

    // invoke
    client.get().uri("/parent/$puid/$upperId")
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON_UTF8)
      .expectBody()
      .jsonPath("$[0].puid").isEqualTo(puid)
      .jsonPath("$[0].upperId").isEqualTo(upperId.toString())

    // verify
    verify(service).find(puid, upperId)
  }

  @Test
  fun failedByPermissionDenied() {
    // mock
    val puid = "puid1"
    val upperId = "1"
    `when`(service.find(puid, upperId)).thenReturn(Flux.error(PermissionDeniedException()))

    // invoke and verify
    client.get().uri("/parent/$puid/$upperId").exchange().expectStatus().isForbidden
    verify(service).find(puid, upperId)
  }
}