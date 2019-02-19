package tech.simter.file.rest.webflux.handler

import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.core.io.ClassPathResource
import org.springframework.http.MediaType
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import org.springframework.test.web.reactive.server.WebTestClient.bindToRouterFunction
import org.springframework.web.reactive.config.EnableWebFlux
import org.springframework.web.reactive.function.server.RouterFunctions.route
import reactor.core.publisher.Mono
import tech.simter.exception.NotFoundException
import tech.simter.file.dto.AttachmentDto
import tech.simter.file.rest.webflux.handler.ReuploadFileByStreamHandler.Companion.REQUEST_PREDICATE
import tech.simter.file.service.AttachmentService
import java.util.*

/**
 * Test [ReuploadFileByStreamHandler].
 *
 * @author zh
 */
@SpringJUnitConfig(ReuploadFileByStreamHandler::class)
@EnableWebFlux
@MockBean(AttachmentService::class)
@SpyBean(ReuploadFileByStreamHandler::class)
internal class ReuploadFileByStreamHandlerTest @Autowired constructor(
  private val service: AttachmentService,
  handler: ReuploadFileByStreamHandler
) {
  private val client = bindToRouterFunction(route(REQUEST_PREDICATE, handler)).build()

  @Test
  fun uploadByNofileName() {
    // mock
    val file = ClassPathResource("logback-test.xml")
    val fileData = file.file.readBytes()
    val id = UUID.randomUUID().toString()
    val fileSize = file.contentLength()
    val attachment = AttachmentDto().also {
      it.id = id
      it.size = fileSize
    }
    `when`(service.reuploadFile(attachment, fileData)).thenReturn(Mono.empty())

    // invoke
    client.patch().uri("/$id")
      .contentType(MediaType.APPLICATION_OCTET_STREAM)
      .contentLength(fileSize)
      .syncBody(file.file.readBytes())
      .exchange()
      .expectStatus().isNoContent

    // verify
    verify(service).reuploadFile(attachment, fileData)
  }

  @Test
  fun reuploadByHasfileName() {
    // mock
    val name = "logback-test"
    val ext = "xml"
    val file = ClassPathResource("$name.$ext")
    val fileData = file.file.readBytes()
    val id = UUID.randomUUID().toString()
    val fileSize = file.contentLength()
    val attachment = AttachmentDto().also {
      it.id = id
      it.size = fileSize
      it.type = ext
      it.name = name
    }
    `when`(service.reuploadFile(attachment, fileData)).thenReturn(Mono.empty())

    // invoke
    client.patch().uri("/$id")
      .header("Content-Disposition", "attachment; name=\"filedata\"; filename=\"$name.$ext\"")
      .contentType(MediaType.APPLICATION_OCTET_STREAM)
      .contentLength(fileSize)
      .syncBody(file.file.readBytes())
      .exchange()
      .expectStatus().isNoContent

    // verify
    verify(service).reuploadFile(attachment, fileData)
  }

  @Test
  fun `Found nothing`() {
    // mock
    val name = "logback-test"
    val ext = "xml"
    val file = ClassPathResource("$name.$ext")
    val fileData = file.file.readBytes()
    val id = UUID.randomUUID().toString()
    val fileSize = file.contentLength()
    val attachment = AttachmentDto().also {
      it.id = id
      it.size = fileSize
      it.type = ext
      it.name = name
    }
    `when`(service.reuploadFile(attachment, fileData)).thenReturn(Mono.error(NotFoundException("")))

    // invoke
    client.patch().uri("/$id")
      .header("Content-Disposition", "attachment; name=\"filedata\"; filename=\"$name.$ext\"")
      .contentType(MediaType.APPLICATION_OCTET_STREAM)
      .contentLength(fileSize)
      .syncBody(fileData)
      .exchange()
      .expectStatus().isNotFound

    // verify
    verify(service).reuploadFile(attachment, fileData)
  }
}