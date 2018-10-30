package tech.simter.file.rest.webflux.handler

import com.nhaarman.mockito_kotlin.any
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.core.io.ClassPathResource
import org.springframework.http.MediaType
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import org.springframework.test.web.reactive.server.WebTestClient.bindToRouterFunction
import org.springframework.web.reactive.config.EnableWebFlux
import org.springframework.web.reactive.function.server.RouterFunctions.route
import reactor.core.publisher.Mono
import tech.simter.exception.NotFoundException
import tech.simter.file.po.Attachment
import tech.simter.file.rest.webflux.handler.ReuploadFileByStreamHandler.Companion.REQUEST_PREDICATE
import tech.simter.file.service.AttachmentService
import java.io.File
import java.io.IOException
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
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
@TestPropertySource(properties = ["simter.file.root=target/files"])
internal class ReuploadFileByStreamHandlerTest @Autowired constructor(
  private val service: AttachmentService,
  @Value("\${simter.file.root}") private val fileRootDir: String,
  private val reuploadFileByStreamHandler: ReuploadFileByStreamHandler
) {
  private val client = bindToRouterFunction(route(REQUEST_PREDICATE, reuploadFileByStreamHandler)).build()

  @Test
  @Throws(IOException::class)
  fun uploadByNofileName() {
    // mock MultipartBody
    val now = OffsetDateTime.now()
    val id = UUID.randomUUID().toString()
    val file = ClassPathResource("logback-test.xml")
    val parentPath = "parent-path7"
    val oldPath = "${now.minusDays(1).format(DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss"))}-$id.xml"
    // mock service.create return value
    val fileSize = file.contentLength()
    val oldAttachment = Attachment(
      id = id, path = oldPath, name = "logback-test", type = "xml",
      size = fileSize, createOn = now, creator = "Simter", modifyOn = now,
      modifier = "Simter", puid = "", upperId = UUID.randomUUID().toString()
    )
    `when`(service.getFullPath(id)).thenReturn(Mono.just("$parentPath/$oldPath"))
    `when`(service.get(id)).thenReturn(Mono.just(oldAttachment))
    `when`(service.save(any())).thenReturn(Mono.empty())

    // invoke request
    client.patch().uri("/$id")
      .contentType(MediaType.APPLICATION_OCTET_STREAM)
      .contentLength(fileSize)
      .syncBody(file.file.readBytes())
      .exchange()
      .expectStatus().isNoContent

    // 1. verify service.save method invoked
    verify(service).getFullPath(id)
    verify(service).get(id)
    verify(service).save(any())

    // 2. verify the saved file exists
    val files = File("$fileRootDir/$parentPath").listFiles()
    assertNotNull(files)
    assertTrue(files.isNotEmpty())
    var actualFile: File? = null
    for (f in files) {
      // extract dateTime and id from fileName: yyyyMMddTHHmmss-{id}.{type}
      val index = f.name.indexOf("-")
      val dateTime = LocalDateTime.parse(f.name.substring(0, index),
        DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss"))
      val uuid = f.name.substring(index + 1, f.name.lastIndexOf("."))
      if (id == uuid && !dateTime.isAfter(now.toLocalDateTime())) {
        actualFile = f
        break
      }
    }
    assertNotNull(actualFile)

    // 3. verify the saved file size
    assertEquals(actualFile!!.length(), fileSize)

    // 4. TODO verify the attachment
  }

  @Test
  @Throws(IOException::class)
  fun reuploadByHasfileName() {
    // mock MultipartBody
    val name = "logback-test"
    val ext = "xml"
    val file = ClassPathResource("$name.$ext")
    val now = OffsetDateTime.now()
    val id = UUID.randomUUID().toString()
    val parentPath = "parent-path9"
    val oldPath = "${now.minusDays(1).format(DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss"))}-$id.xml"
    // mock service.create return value

    val fileSize = file.contentLength()
    val oldAttachment = Attachment(
      id = id, path = oldPath, name = "old", type = "xml",
      size = fileSize, createOn = now, creator = "Simter", modifyOn = now,
      modifier = "Simter", puid = "", upperId = UUID.randomUUID().toString()
    )
    `when`(service.getFullPath(id)).thenReturn(Mono.just("$parentPath/$oldPath"))
    `when`(service.get(id)).thenReturn(Mono.just(oldAttachment))
    `when`(service.save(any())).thenReturn(Mono.empty())

    // invoke request
    client.patch().uri("/$id")
      .header("Content-Disposition", "attachment; name=\"filedata\"; filename=\"$name.$ext\"")
      .contentType(MediaType.APPLICATION_OCTET_STREAM)
      .contentLength(fileSize)
      .syncBody(file.file.readBytes())
      .exchange()
      .expectStatus().isNoContent

    // 1. verify service.save method invoked
    verify(service).getFullPath(id)
    verify(service).get(id)
    verify(service).save(any())

    // 2. verify the saved file exists
    val files = File("$fileRootDir/$parentPath").listFiles()
    assertNotNull(files)
    assertTrue(files.isNotEmpty())
    var actualFile: File? = null
    for (f in files) {
      // extract dateTime and id from fileName: yyyyMMddTHHmmss-{id}.{type}
      val index = f.name.indexOf("-")
      val dateTime = LocalDateTime.parse(f.name.substring(0, index),
        DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss"))
      val uuid = f.name.substring(index + 1, f.name.lastIndexOf("."))
      // Verify that the old file is deleted.
      assertNotEquals(f.name, oldPath)
      if (id == uuid && !dateTime.isAfter(now.toLocalDateTime())) {
        actualFile = f
        break
      }
    }
    assertNotNull(actualFile)

    // 3. verify the saved file size
    assertEquals(actualFile!!.length(), fileSize)

    // 4. TODO verify the attachment
  }

  @Test
  fun `Found nothing`() {
    // mock MultipartBody
    val name = "logback-test"
    val ext = "xml"
    val file = ClassPathResource("$name.$ext")
    // mock service.create return value
    val id = UUID.randomUUID().toString()
    val fileSize = file.contentLength()
    `when`(service.get(id)).thenReturn(Mono.error(NotFoundException("")))

    // invoke request
    client.patch().uri("/$id")
      .header("Content-Disposition", "attachment; name=\"filedata\"; filename=\"$$name.$ext\"")
      .contentType(MediaType.APPLICATION_OCTET_STREAM)
      .contentLength(fileSize)
      .syncBody(file.file.readBytes())
      .exchange()
      .expectStatus().isNotFound

    // verify service.save method invoked
    verify(service).get(id)
  }
}