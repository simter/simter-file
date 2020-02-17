package tech.simter.file.rest.webflux.handler

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.core.io.ClassPathResource
import org.springframework.http.MediaType.APPLICATION_OCTET_STREAM
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import tech.simter.exception.NotFoundException
import tech.simter.exception.PermissionDeniedException
import tech.simter.file.core.AttachmentService
import tech.simter.file.rest.webflux.UnitTestConfiguration
import java.io.File
import java.util.*

/**
 * Test UploadFileByStreamHandler.
 *
 * @author JW
 * @author zh
 * @author RJ
 */
@SpringJUnitConfig(UnitTestConfiguration::class)
@WebFluxTest
@SpyBean(UploadFileByStreamHandler::class)
@TestPropertySource(properties = ["simter.file.root=target/files"])
class UploadFileByStreamHandlerTest @Autowired constructor(
  private val client: WebTestClient,
  private val service: AttachmentService,
  @Value("\${simter.file.root}") private val fileRootDir: String,
  private val handler: UploadFileByStreamHandler
) {
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
    val beCreatedFile = File("$fileRootDir/text.xml")
    beCreatedFile.parentFile.mkdirs()
    `when`(service.uploadFile(any(), any())).thenReturn(Mono.empty())
    doReturn(id).`when`(handler).newId()

    // invoke request
    client.post().uri("/?puid=$puid&upper=$upperId&filename=$fileName")
      .contentType(APPLICATION_OCTET_STREAM)
      .contentLength(fileSize)
      .bodyValue(fileData)
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
      .bodyValue(fileData)
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

  @Test
  fun failedByPermissionDenied() {
    // mock
    val fileName = "logback-test.xml"
    val file = ClassPathResource(fileName)
    val fileData = file.file.readBytes()
    val upperId = UUID.randomUUID().toString()
    val id = UUID.randomUUID().toString()
    val fileSize = file.contentLength()
    `when`(service.uploadFile(any(), any())).thenReturn(Mono.error(PermissionDeniedException()))
    doReturn(id).`when`(handler).newId()

    // invoke request
    client.post().uri("/?upper=$upperId&filename=$fileName")
      .contentType(APPLICATION_OCTET_STREAM)
      .contentLength(fileSize)
      .bodyValue(fileData)
      .exchange()
      .expectStatus().isForbidden

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