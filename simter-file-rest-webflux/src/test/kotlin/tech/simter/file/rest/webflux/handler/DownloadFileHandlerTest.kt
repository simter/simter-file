package tech.simter.file.rest.webflux.handler

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.core.io.FileSystemResource
import org.springframework.http.MediaType.APPLICATION_XML_VALUE
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import tech.simter.exception.PermissionDeniedException
import tech.simter.file.core.AttachmentService
import tech.simter.file.core.domain.Attachment
import tech.simter.file.impl.domain.AttachmentImpl
import tech.simter.file.rest.webflux.UnitTestConfiguration
import java.time.OffsetDateTime
import java.util.*

/**
 * Test [DownloadFileHandler].
 *
 * @author JF
 * @author RJ
 * @author zh
 */
@SpringJUnitConfig(UnitTestConfiguration::class)
@WebFluxTest
@TestPropertySource(properties = ["simter.file.root=src/test"])
class DownloadFileHandlerTest @Autowired constructor(
  private val client: WebTestClient,
  private val service: AttachmentService,
  @Value("\${simter.file.root}") private val fileRootDir: String
) {
  @Test
  fun found() {
    // mock service return value
    val name = "logback-test"
    val ext = "xml"
    val fileName = "$name.$ext"
    val id = UUID.randomUUID().toString()
    val now = OffsetDateTime.now()
    val fileSize = FileSystemResource("$fileRootDir/resources/$fileName").contentLength()
    val attachment: Attachment = AttachmentImpl(id, "$name.$ext", name, ext, fileSize,
      now, "Simter", now, "Simter", "0")
    val expected = Mono.just(attachment)
    `when`(service.get(id)).thenReturn(expected)
    `when`(service.getFullPath(id)).thenReturn("resources/$name.$ext".toMono())

    // invoke request
    val result = client.get().uri("/$id")
      .exchange()
      .expectStatus().isOk
      .expectHeader().valueEquals("Content-Type", APPLICATION_XML_VALUE)
      .expectHeader().valueEquals("Content-Length", fileSize.toString())
      .expectHeader().valueEquals("Content-Disposition", "attachment; filename=\"$fileName\"")
      .expectBody().returnResult()

    // verify response body
    assertNotNull(result.responseBody)
    assertEquals(result.responseBody!!.size.toLong(), fileSize)

    // verify method service.get invoked
    verify(service).get(id)
    verify(service).getFullPath(id)
  }

  @Test
  fun notFound() {
    // mock
    val id = UUID.randomUUID().toString()
    `when`(service.get(id)).thenReturn(Mono.empty())
    `when`(service.getFullPath(id)).thenReturn(Mono.empty())

    // invoke
    client.get().uri("/$id").exchange().expectStatus().isNotFound

    // verify
    verify(service).get(id)
    verify(service).getFullPath(id)
  }

  @Test
  fun failedByPermissionDenied() {
    // mock
    val id = UUID.randomUUID().toString()
    `when`(service.get(id)).thenReturn(Mono.error(PermissionDeniedException()))

    // invoke
    client.get().uri("/$id").exchange().expectStatus().isForbidden

    // verify
    verify(service).get(id)
  }
}