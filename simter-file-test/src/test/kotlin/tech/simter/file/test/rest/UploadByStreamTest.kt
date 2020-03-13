package tech.simter.file.test.rest

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.io.ClassPathResource
import org.springframework.http.MediaType.APPLICATION_OCTET_STREAM
import org.springframework.http.MediaType.TEXT_PLAIN
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody

/**
 * Test upload file by ajax.
 *
 * @author RJ
 */
@SpringBootTest(classes = [UnitTestConfiguration::class])
class UploadByStreamTest @Autowired constructor(
  private val client: WebTestClient
) {
  @Test
  fun specifyParams() {
    // prepare data
    val fileName = "logback-test.xml"
    val file = ClassPathResource(fileName)
    val fileData = file.file.readBytes()
    val fileSize = file.contentLength()

    // upload file
    client.post().uri {
        it.path("/")
          .queryParam("module", "/test/")
          .queryParam("name", "logback-test")
          .queryParam("type", "xml")
          .build()
      }
      .contentType(APPLICATION_OCTET_STREAM)
      .contentLength(fileSize)
      .bodyValue(fileData)
      .exchange()
      .expectStatus().isCreated
      .expectHeader().contentTypeCompatibleWith(TEXT_PLAIN)
      .expectBody<String>().consumeWith {
        assertThat(it.responseBody).hasSizeGreaterThan(0)
      }
  }

  @Test
  fun noParams() {
    // prepare data
    val fileName = "logback-test.xml"
    val file = ClassPathResource(fileName)
    val fileData = file.file.readBytes()
    val fileSize = file.contentLength()

    // upload file
    client.post().uri("/")
      .contentType(APPLICATION_OCTET_STREAM)
      .contentLength(fileSize)
      .bodyValue(fileData)
      .exchange()
      .expectStatus().isCreated
      .expectHeader().contentTypeCompatibleWith(TEXT_PLAIN)
      .expectBody<String>().consumeWith {
        assertThat(it.responseBody).hasSizeGreaterThan(0)
      }
  }
}