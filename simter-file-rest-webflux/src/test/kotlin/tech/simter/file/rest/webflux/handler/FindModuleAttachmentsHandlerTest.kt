package tech.simter.file.rest.webflux.handler

import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.core.publisher.Flux
import tech.simter.exception.PermissionDeniedException
import tech.simter.file.core.AttachmentService
import tech.simter.file.core.domain.Attachment
import tech.simter.file.impl.domain.AttachmentImpl
import tech.simter.file.rest.webflux.UnitTestConfiguration
import java.time.OffsetDateTime

/**
 * Test [FindModuleAttachmentsHandler].
 *
 * @author JW
 * @author zh
 * @author RJ
 */
@SpringJUnitConfig(UnitTestConfiguration::class)
@WebFluxTest
class FindModuleAttachmentsHandlerTest @Autowired constructor(
  private val client: WebTestClient,
  private val service: AttachmentService
) {
  @Test
  fun file() {
    // mock
    val puid = "puid1"
    val upperId = "1"
    val now = OffsetDateTime.now()
    `when`<Flux<Attachment>>(service.find(puid, upperId)).thenReturn(Flux.just(
      AttachmentImpl(
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
      .expectHeader().contentType(APPLICATION_JSON)
      .expectBody()
      .jsonPath("$[0].puid").isEqualTo(puid)
      .jsonPath("$[0].upperId").isEqualTo(upperId)

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