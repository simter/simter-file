package tech.simter.file.test.starter

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.io.ClassPathResource
import org.springframework.http.MediaType.APPLICATION_OCTET_STREAM
import org.springframework.test.web.reactive.server.WebTestClient

/**
 * Test `POST /` through ajax.
 *
 * @author RJ
 */
@SpringBootTest(classes = [IntegrationTestConfiguration::class])
class UploadFileByStreamTest @Autowired constructor(
  private val client: WebTestClient
) {
  @Test
  fun test() {
    // mock
    val fileName = "logback-test.xml"
    val file = ClassPathResource(fileName)
    val fileData = file.file.readBytes()
    val upperId = "EMPTY"
    val fileSize = file.contentLength()
    val puid = "text"

    // upload file
    client.post().uri("/?puid=$puid&upper=$upperId&filename=$fileName")
      .contentType(APPLICATION_OCTET_STREAM)
      .contentLength(fileSize)
      .bodyValue(fileData)
      .exchange()
      .expectStatus().isCreated
      .expectHeader().value("Location") {
        assertThat(it).startsWith("/")
          .hasSizeGreaterThan(1)
        // TODO verify file created
        //println("Location=$it")
      }
  }
}