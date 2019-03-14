package tech.simter.file.rest.webflux.handler

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.argThat
import com.nhaarman.mockito_kotlin.doReturn
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.core.io.ClassPathResource
import org.springframework.http.MediaType.MULTIPART_FORM_DATA
import org.springframework.http.client.MultipartBodyBuilder
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import org.springframework.test.web.reactive.server.WebTestClient.bindToRouterFunction
import org.springframework.web.reactive.config.EnableWebFlux
import org.springframework.web.reactive.function.server.RouterFunctions.route
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import tech.simter.exception.NotFoundException
import tech.simter.exception.PermissionDeniedException
import tech.simter.file.rest.webflux.handler.UploadFileByFormHandler.Companion.REQUEST_PREDICATE
import tech.simter.file.service.AttachmentService
import java.io.File
import java.util.*

/**
 * Test [UploadFileByFormHandler].
 *
 * @author JF
 * @author RJ
 * @author zh
 */
@SpringJUnitConfig(UploadFileByFormHandler::class)
@EnableWebFlux
@MockBean(AttachmentService::class)
@SpyBean(UploadFileByFormHandler::class)
@TestPropertySource(properties = ["simter.file.root=target/files"])
internal class UploadFileByFormHandlerTest @Autowired constructor(
  private val service: AttachmentService,
  @Value("\${simter.file.root}") private val fileRootDir: String,
  private val byFormHandler: UploadFileByFormHandler
) {
  private val client = bindToRouterFunction(route(REQUEST_PREDICATE, byFormHandler)).build()

  @AfterEach
  fun clean() {
    File(fileRootDir).deleteRecursively()
  }

  @Test
  fun success() {
    // mock
    val fileName = "logback-test.xml"
    val file = ClassPathResource(fileName)
    val upperId = UUID.randomUUID().toString()
    val id = UUID.randomUUID().toString()
    val fileSize = file.contentLength()
    val puid = "text"
    val beCreatedFile = File("fileRootDir/text.xml")
    val parts = MultipartBodyBuilder().also {
      it.part("fileData", file)
      it.part("upperId", upperId)
      it.part("puid", puid)
    }.build()
    beCreatedFile.parentFile.mkdirs()
    `when`(service.uploadFile(any(), any())).thenReturn(Mono.empty())
    doReturn(id).`when`(byFormHandler).newId()

    // invoke request
    client.post().uri("/")
      .contentType(MULTIPART_FORM_DATA)
      .contentLength(fileSize)
      .syncBody(parts)
      .exchange()
      .expectStatus().isNoContent
      .expectHeader().valueEquals("Location", "/$id")

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
    val upperId = UUID.randomUUID().toString()
    val id = UUID.randomUUID().toString()
    val fileSize = file.contentLength()
    val beCreatedFile = File("fileRootDir/text.xml")
    val parts = MultipartBodyBuilder().also {
      it.part("fileData", file)
      it.part("upperId", upperId)
    }.build()
    beCreatedFile.parentFile.mkdirs()
    `when`(service.uploadFile(any(), any())).thenReturn(Mono.error(NotFoundException("not Found upper")))
    doReturn(id).`when`(byFormHandler).newId()

    // invoke request
    client.post().uri("/")
      .contentType(MULTIPART_FORM_DATA)
      .contentLength(fileSize)
      .syncBody(parts)
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
    val upperId = UUID.randomUUID().toString()
    val id = UUID.randomUUID().toString()
    val fileSize = file.contentLength()
    val beCreatedFile = File("fileRootDir/text.xml")
    val parts = MultipartBodyBuilder().also {
      it.part("fileData", file)
      it.part("upperId", upperId)
    }.build()
    beCreatedFile.parentFile.mkdirs()
    `when`(service.uploadFile(any(), any())).thenReturn(Mono.error(PermissionDeniedException()))
    doReturn(id).`when`(byFormHandler).newId()

    // invoke request
    client.post().uri("/")
      .contentType(MULTIPART_FORM_DATA)
      .contentLength(fileSize)
      .syncBody(parts)
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