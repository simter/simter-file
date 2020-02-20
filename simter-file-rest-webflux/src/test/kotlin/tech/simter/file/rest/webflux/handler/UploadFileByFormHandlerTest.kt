package tech.simter.file.rest.webflux.handler

import com.ninjasquad.springmockk.SpykBean
import io.mockk.every
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.core.io.ClassPathResource
import org.springframework.http.MediaType.MULTIPART_FORM_DATA
import org.springframework.http.client.MultipartBodyBuilder
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.core.publisher.Mono
import reactor.kotlin.test.test
import tech.simter.exception.NotFoundException
import tech.simter.exception.PermissionDeniedException
import tech.simter.file.core.AttachmentService
import tech.simter.file.rest.webflux.UnitTestConfiguration
import java.io.File
import java.util.*

/**
 * Test [UploadFileByFormHandler].
 *
 * @author JF
 * @author RJ
 * @author zh
 */
@SpringJUnitConfig(UnitTestConfiguration::class)
@WebFluxTest
@SpykBean(UploadFileByFormHandler::class)
@TestPropertySource(properties = ["simter.file.root=target/files"])
class UploadFileByFormHandlerTest @Autowired constructor(
  private val client: WebTestClient,
  private val service: AttachmentService,
  @Value("\${simter.file.root}") private val fileRootDir: String,
  private val handler: UploadFileByFormHandler
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
    val upperId = UUID.randomUUID().toString()
    val id = UUID.randomUUID().toString()
    val fileSize = file.contentLength()
    val puid = "text"
    val beCreatedFile = File("$fileRootDir/text.xml")
    val parts = MultipartBodyBuilder().also {
      it.part("fileData", file)
      it.part("upperId", upperId)
      it.part("puid", puid)
    }.build()
    beCreatedFile.parentFile.mkdirs()
    every { service.uploadFile(any(), any()) } returns Mono.empty()
    every { handler.newId() } returns id

    // invoke request
    client.post().uri("/")
      .contentType(MULTIPART_FORM_DATA)
      .contentLength(fileSize)
      .bodyValue(parts)
      .exchange()
      .expectStatus().isNoContent
      .expectHeader().valueEquals("Location", "/$id")

    // verify
    verify {
      service.uploadFile(
        match {
          assertEquals(id, it.id)
          assertEquals(upperId, it.upperId)
          assertEquals(fileSize, it.size)
          assertEquals(puid, it.puid)
          true
        },
        match {
          it(beCreatedFile).test().verifyComplete()
          true
        }
      )
    }
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
    val beCreatedFile = File("$fileRootDir/text.xml")
    val parts = MultipartBodyBuilder().also {
      it.part("fileData", file)
      it.part("upperId", upperId)
    }.build()
    beCreatedFile.parentFile.mkdirs()
    every { service.uploadFile(any(), any()) } returns Mono.error(NotFoundException("not Found upper"))
    every { handler.newId() } returns id

    // invoke request
    client.post().uri("/")
      .contentType(MULTIPART_FORM_DATA)
      .contentLength(fileSize)
      .bodyValue(parts)
      .exchange()
      .expectStatus().isNotFound

    // verify
    verify {
      service.uploadFile(
        match {
          assertEquals(id, it.id)
          assertEquals(upperId, it.upperId)
          assertEquals(fileSize, it.size)
          assertNull(it.puid)
          true
        },
        any()
      )
    }
  }

  @Test
  fun failedByPermissionDenied() {
    // mock
    val fileName = "logback-test.xml"
    val file = ClassPathResource(fileName)
    val upperId = UUID.randomUUID().toString()
    val id = UUID.randomUUID().toString()
    val fileSize = file.contentLength()
    val beCreatedFile = File("$fileRootDir/text.xml")
    val parts = MultipartBodyBuilder().also {
      it.part("fileData", file)
      it.part("upperId", upperId)
    }.build()
    beCreatedFile.parentFile.mkdirs()
    every { service.uploadFile(any(), any()) } returns Mono.error(PermissionDeniedException())
    every { handler.newId() } returns id

    // invoke request
    client.post().uri("/")
      .contentType(MULTIPART_FORM_DATA)
      .contentLength(fileSize)
      .bodyValue(parts)
      .exchange()
      .expectStatus().isForbidden

    // verify
    verify {
      service.uploadFile(
        match {
          assertEquals(id, it.id)
          assertEquals(upperId, it.upperId)
          assertEquals(fileSize, it.size)
          assertNull(it.puid)
          true
        },
        any()
      )
    }
  }
}