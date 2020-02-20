package tech.simter.file.rest.webflux.handler

import io.mockk.every
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.core.io.ClassPathResource
import org.springframework.http.MediaType.APPLICATION_OCTET_STREAM
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.core.publisher.Mono
import tech.simter.exception.NotFoundException
import tech.simter.exception.PermissionDeniedException
import tech.simter.file.core.AttachmentService
import tech.simter.file.impl.domain.AttachmentUpdateInfoImpl
import tech.simter.file.rest.webflux.TestHelper.randomAttachmentId
import tech.simter.file.rest.webflux.UnitTestConfiguration
import java.util.*

/**
 * Test [ReuploadFileByStreamHandler].
 *
 * @author zh
 * @author RJ
 */
@SpringJUnitConfig(UnitTestConfiguration::class)
@WebFluxTest
class ReuploadFileByStreamHandlerTest @Autowired constructor(
  private val client: WebTestClient,
  private val service: AttachmentService
) {
  @Test
  fun `reupload by no file name`() {
    // mock
    val file = ClassPathResource("logback-test.xml")
    val fileData = file.file.readBytes()
    val id = randomAttachmentId()
    val fileSize = file.contentLength()
    val info = AttachmentUpdateInfoImpl().apply {
      size = fileSize
    }
    every { service.reuploadFile(id = id, fileData = fileData, info = info) } returns Mono.empty()

    // invoke
    client.patch().uri("/$id")
      .contentType(APPLICATION_OCTET_STREAM)
      .contentLength(fileSize)
      .bodyValue(file.file.readBytes())
      .exchange()
      .expectStatus().isNoContent

    // verify
    verify { service.reuploadFile(id = id, fileData = fileData, info = info) }
  }

  @Test
  fun `reupload by has file name`() {
    // mock
    val name = "logback-test"
    val ext = "xml"
    val file = ClassPathResource("$name.$ext")
    val fileData = file.file.readBytes()
    val id = UUID.randomUUID().toString()
    val fileSize = file.contentLength()
    val info = AttachmentUpdateInfoImpl().also {
      it.size = fileSize
      it.type = ext
      it.name = name
    }
    every { service.reuploadFile(id = id, fileData = fileData, info = info) } returns Mono.empty()

    // invoke
    client.patch().uri("/$id")
      .header("Content-Disposition", "attachment; name=\"filedata\"; filename=\"$name.$ext\"")
      .contentType(APPLICATION_OCTET_STREAM)
      .contentLength(fileSize)
      .bodyValue(file.file.readBytes())
      .exchange()
      .expectStatus().isNoContent

    // verify
    verify { service.reuploadFile(id = id, fileData = fileData, info = info) }
  }

  @Test
  fun `not found exception`() {
    // mock
    val name = "logback-test"
    val ext = "xml"
    val file = ClassPathResource("$name.$ext")
    val fileData = file.file.readBytes()
    val id = UUID.randomUUID().toString()
    val fileSize = file.contentLength()
    val info = AttachmentUpdateInfoImpl().also {
      it.size = fileSize
      it.type = ext
      it.name = name
    }
    every { service.reuploadFile(id = id, fileData = fileData, info = info) } returns Mono.error(NotFoundException(""))

    // invoke
    client.patch().uri("/$id")
      .header("Content-Disposition", "attachment; name=\"filedata\"; filename=\"$name.$ext\"")
      .contentType(APPLICATION_OCTET_STREAM)
      .contentLength(fileSize)
      .bodyValue(fileData)
      .exchange()
      .expectStatus().isNotFound

    // verify
    verify { service.reuploadFile(id = id, fileData = fileData, info = info) }
  }

  @Test
  fun `failed by permission denied`() {
    // mock
    val name = "logback-test"
    val ext = "xml"
    val file = ClassPathResource("$name.$ext")
    val fileData = file.file.readBytes()
    val id = UUID.randomUUID().toString()
    val fileSize = file.contentLength()
    val info = AttachmentUpdateInfoImpl().also {
      it.size = fileSize
      it.type = ext
      it.name = name
    }
    every { service.reuploadFile(id = id, fileData = fileData, info = info) } returns Mono.error(PermissionDeniedException())

    // invoke
    client.patch().uri("/$id")
      .header("Content-Disposition", "attachment; name=\"filedata\"; filename=\"$name.$ext\"")
      .contentType(APPLICATION_OCTET_STREAM)
      .contentLength(fileSize)
      .bodyValue(fileData)
      .exchange()
      .expectStatus().isForbidden

    // verify
    verify { service.reuploadFile(id = id, fileData = fileData, info = info) }
  }
}