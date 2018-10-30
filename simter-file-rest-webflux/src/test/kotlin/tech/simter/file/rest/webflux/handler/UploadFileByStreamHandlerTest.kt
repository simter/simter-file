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
import tech.simter.file.rest.webflux.handler.UploadFileByStreamHandler.Companion.REQUEST_PREDICATE
import tech.simter.file.service.AttachmentService
import java.io.File
import java.io.IOException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit.SECONDS
import java.util.*

/**
 * Test UploadFileByStreamHandler.
 *
 * @author JW
 */
@SpringJUnitConfig(UploadFileByStreamHandler::class)
@EnableWebFlux
@MockBean(AttachmentService::class)
@SpyBean(UploadFileByStreamHandler::class)
@TestPropertySource(properties = ["simter.file.root=target/files"])
internal class UploadFileByStreamHandlerTest @Autowired constructor(
  private val service: AttachmentService,
  @Value("\${simter.file.root}") private val fileRootDir: String,
  private val uploadFileByStreamHandler: UploadFileByStreamHandler
) {
  private val client = bindToRouterFunction(route(REQUEST_PREDICATE, uploadFileByStreamHandler)).build()

  @Test
  @Throws(IOException::class)
  fun upload() {
    // mock MultipartBody
    val name = "logback-test"
    val ext = "xml"
    val file = ClassPathResource("$name.$ext")

    // mock service.create return value
    val id = UUID.randomUUID().toString()
    val fileSize = file.contentLength()
    `when`(service.save(any())).thenReturn(Mono.empty())

    // mock uploadFileByStreamHandler.newId return value
    `when`(uploadFileByStreamHandler.newId()).thenReturn(id)

    // invoke request
    val now = LocalDateTime.now().truncatedTo(SECONDS)
    client.post().uri("/?puid=puid1&upperId=1")
      .header("Content-Disposition","attachment; name=\"filedata\"; filename=\"$$name.$ext\"")
      .contentType(MediaType.APPLICATION_OCTET_STREAM)
      .contentLength(fileSize)
      .syncBody(file.file.readBytes())
      .exchange()
      .expectStatus().isNoContent
      .expectHeader().valueEquals("Location", "/$id")

    // 1. verify service.save method invoked
    verify(service).save(any())

    // 2. verify the saved file exists
    val yyyyMM = now.format(DateTimeFormatter.ofPattern("yyyyMM"))
    val files = File("$fileRootDir/$yyyyMM").listFiles()
    assertNotNull(files)
    assertTrue(files.isNotEmpty())
    var actualFile: File? = null
    for (f in files) {
      // extract dateTime and id from fileName: yyyyMMddTHHmmss-{id}.{type}
      val index = f.name.indexOf("-")
      val dateTime = LocalDateTime.parse(f.name.substring(0, index),
        DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss"))
      val uuid = f.name.substring(index + 1, f.name.lastIndexOf("."))
      if (id == uuid && !dateTime.isBefore(now)) {
        actualFile = f
        break
      }
    }
    assertNotNull(actualFile)

    // 3. verify the saved file size
    assertEquals(actualFile!!.length(), fileSize)

    // 4. TODO verify the attachment
  }
}