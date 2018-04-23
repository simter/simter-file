package tech.simter.file.rest.webflux.handler

import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.http.MediaType
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import org.springframework.test.web.reactive.server.WebTestClient.bindToRouterFunction
import org.springframework.web.reactive.config.EnableWebFlux
import org.springframework.web.reactive.function.server.RouterFunctions.route
import reactor.core.publisher.Mono
import tech.simter.file.po.Attachment
import tech.simter.file.rest.webflux.handler.AttachmentViewHandler.Companion.REQUEST_PREDICATE
import tech.simter.file.service.AttachmentService
import java.time.OffsetDateTime
import java.util.*

/**
 * Test AttachmentViewHandler.
 *
 * @author JF
 */
@SpringJUnitConfig(AttachmentViewHandler::class)
@EnableWebFlux
@MockBean(AttachmentService::class)
internal class AttachmentViewHandlerTest @Autowired constructor(
  private val service: AttachmentService,
  handler: AttachmentViewHandler
) {
  private val client = bindToRouterFunction(route(REQUEST_PREDICATE, handler)).build()

  @Test
  fun fileView() {
    // mock
    val pageNo = 0
    val pageSize = 25
    val id = UUID.randomUUID().toString()
    val list = ArrayList<Attachment>()
    list.add(Attachment(id, "/path", "name", "ext", 100, OffsetDateTime.now(), "Simter", "0", 0))
    val pageable = PageRequest.of(pageNo, pageSize)
    `when`<Mono<Page<Attachment>>>(service.find(pageable)).thenReturn(Mono.just<Page<Attachment>>(PageImpl(list, pageable, list.size.toLong())))

    // invoke
    client.get().uri("/attachment?page-no=0&page-size=25")
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON_UTF8)
      .expectBody()
      .jsonPath("$.count").isEqualTo(list.size) // verify count
      .jsonPath("$.pageNo").isEqualTo(pageNo)     // verify page-no
      .jsonPath("$.pageSize").isEqualTo(pageSize) // verify page-size
      .jsonPath("$.rows[0].id").isEqualTo(id)    // verify Attachment.id

    // verify
    verify(service).find(pageable)
  }
}