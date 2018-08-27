package tech.simter.file.rest.webflux.handler

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.core.io.FileSystemResource
import org.springframework.http.MediaType
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import org.springframework.test.web.reactive.server.WebTestClient.bindToRouterFunction
import org.springframework.web.reactive.config.EnableWebFlux
import org.springframework.web.reactive.function.server.RouterFunctions.route
import reactor.core.publisher.Mono
import tech.simter.file.po.Attachment
import tech.simter.file.rest.webflux.handler.InlineFileHandler.Companion.REQUEST_PREDICATE
import tech.simter.file.service.AttachmentService
import java.time.OffsetDateTime
import java.util.*

/**
 * Test InlineFileHandler.
 *
 * @author JW
 */
@SpringJUnitConfig(InlineFileHandler::class)
@EnableWebFlux
@MockBean(AttachmentService::class)
@TestPropertySource(properties = ["simter.file.root=src/test"])
internal class InlineFileHandlerTest @Autowired constructor(
  private val service: AttachmentService,
  @Value("\${simter.file.root}") private val fileRootDir: String,
  handler: InlineFileHandler
) {
  private val client = bindToRouterFunction(route(REQUEST_PREDICATE, handler)).build()

  @Test
  fun found() {
    // mock service return value
    val name = "logback-test"
    val ext = "xml"
    val fileName = "$name.$ext"
    val id = UUID.randomUUID().toString()
    val fileSize = FileSystemResource("$fileRootDir/resources/$fileName").contentLength()
    val attachment = Attachment(id, "resources/$name.$ext", name, ext, fileSize,
      OffsetDateTime.now(), "Simter", "0", 0)
    val expected = Mono.just(attachment)
    `when`(service.get(id)).thenReturn(expected)

    // invoke request
    val result = client.get().uri("/inline/$id")
      .exchange()
      .expectStatus().isOk
      .expectHeader().valueEquals("Content-Type", MediaType.APPLICATION_XML_VALUE)
      .expectHeader().valueEquals("Content-Length", fileSize.toString())
      .expectHeader().valueEquals("Content-Disposition", "inline; filename=\"$fileName\"")
      .expectBody().returnResult()

    // verify response body
    assertNotNull(result.responseBody)
    assertEquals(result.responseBody!!.size.toLong(), fileSize)

    // verify method service.get invoked
    verify(service).get(id)
  }

  @Test
  fun notFound() {
    // mock
    val id = UUID.randomUUID().toString()
    `when`(service.get(id)).thenReturn(Mono.empty())

    // invoke
    client.get().uri("/inline/$id").exchange().expectStatus().isNotFound

    // verify
    verify(service).get(id)
  }
}