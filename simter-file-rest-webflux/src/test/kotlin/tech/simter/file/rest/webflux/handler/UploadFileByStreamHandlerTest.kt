package tech.simter.file.rest.webflux.handler

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.argThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.core.io.ClassPathResource
import org.springframework.http.MediaType.APPLICATION_OCTET_STREAM
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import org.springframework.test.web.reactive.server.WebTestClient.bindToRouterFunction
import org.springframework.web.reactive.config.EnableWebFlux
import org.springframework.web.reactive.function.server.RouterFunctions.route
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import tech.simter.exception.NotFoundException
import tech.simter.file.rest.webflux.handler.UploadFileByStreamHandler.Companion.REQUEST_PREDICATE
import tech.simter.file.service.AttachmentService
import java.io.File
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue


/**
 * Test UploadFileByStreamHandler.
 *
 * @author JW
 * @author zh
 */
@SpringJUnitConfig(UploadFileByStreamHandler::class)
@EnableWebFlux
@MockBean(AttachmentService::class)
@SpyBean(UploadFileByStreamHandler::class)
@TestPropertySource(properties = ["simter.file.root=target/files"])
internal class UploadFileByStreamHandlerTest @Autowired constructor(
  private val service: AttachmentService,
  @Value("\${simter.file.root}") private val fileRootDir: String,
  private val handler: UploadFileByStreamHandler
) {
  private val client = bindToRouterFunction(route(REQUEST_PREDICATE, handler)).build()

  @AfterEach
  fun clean() {
    File(fileRootDir).deleteRecursively()
  }

  @Test
  fun success() {
    // mock
    val fileName = "logback-test.xml"
    val file = ClassPathResource(fileName)
    val fileData = file.file.readBytes()
    val upperId = UUID.randomUUID().toString()
    val id = UUID.randomUUID().toString()
    val fileSize = file.contentLength()
    val puid = "text"
    val beCreatedFile = File("fileRootDir/text.xml")
    beCreatedFile.parentFile.mkdirs()
    `when`(service.uploadFile(any(), any())).thenReturn(Mono.empty())
    doReturn(id).`when`(handler).newId()

    // invoke request
    client.post().uri("/?puid=$puid&upper=$upperId&filename=$fileName")
      .contentType(APPLICATION_OCTET_STREAM)
      .contentLength(fileSize)
      .syncBody(fileData)
      .exchange()
      .expectStatus().isCreated
      .expectBody().jsonPath("$").isEqualTo(id)

    // verify
    verify(service).uploadFile(argThat {
      assertEquals(id, this.id)
      assertEquals(upperId, this.upperId)
      assertEquals(fileSize, this.size)
      assertEquals(puid, this.puid)
      true
    }, argThat {
      StepVerifier.create(this(beCreatedFile)).verifyComplete()
      true
    })
    assertTrue(beCreatedFile.exists())
  }

  @Test
  fun notFoundUpper() {
    // mock
    val fileName = "logback-test.xml"
    val file = ClassPathResource(fileName)
    val fileData = file.file.readBytes()
    val upperId = UUID.randomUUID().toString()
    val id = UUID.randomUUID().toString()
    val fileSize = file.contentLength()
    `when`(service.uploadFile(any(), any())).thenReturn(Mono.error(NotFoundException("not Found upper")))
    doReturn(id).`when`(handler).newId()

    // invoke request
    client.post().uri("/?upper=$upperId&filename=$fileName")
      .contentType(APPLICATION_OCTET_STREAM)
      .contentLength(fileSize)
      .syncBody(fileData)
      .exchange()
      .expectStatus().isNotFound

    // verify
    verify(service).uploadFile(argThat {
      assertEquals(id, this.id)
      assertEquals(upperId, this.upperId)
      assertEquals(fileSize, this.size)
      assertNull(this.puid)
      true
    }, any())
  }
}