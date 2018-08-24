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
import tech.simter.file.po.Attachment
import tech.simter.file.rest.webflux.handler.FindModuleAttachmentsHandler.Companion.REQUEST_PREDICATE
import tech.simter.file.service.AttachmentService
import java.time.OffsetDateTime

/**
 * Test FindModuleAttachmentsHandler.
 *
 * @author JW
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
    val subgroup: Short = 1
    `when`<Flux<Attachment>>(service.find(puid, subgroup)).thenReturn(Flux.just(
      Attachment(
        id = "1",
        path = "path/",
        name = "name",
        ext = "png",
        size = 100L,
        uploadOn = OffsetDateTime.now(),
        uploader = "uploader",
        puid = puid,
        subgroup = subgroup))
    )

    // invoke
    client.get().uri("/parent/$puid/$subgroup")
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON_UTF8)
      .expectBody()
      .jsonPath("$[0].puid").isEqualTo(puid)
      .jsonPath("$[0].subgroup").isEqualTo(subgroup.toString())

    // verify
    verify(service).find(puid, subgroup)
  }
}