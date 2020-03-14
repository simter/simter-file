package tech.simter.file.test.rest

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.io.ClassPathResource
import org.springframework.http.MediaType.MULTIPART_FORM_DATA
import org.springframework.http.MediaType.TEXT_PLAIN
import org.springframework.http.client.MultipartBodyBuilder
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.web.reactive.function.BodyInserters
import tech.simter.file.test.TestHelper.randomModuleValue

/**
 * Test upload file by traditional form submit.
 *
 * @author RJ
 */
@SpringBootTest(classes = [UnitTestConfiguration::class])
class UploadByFormSubmitTest @Autowired constructor(
  private val client: WebTestClient
) {
  @Test
  fun specifyParams() {
    // prepare data
    val fileName = "logback-test.xml"
    val file = ClassPathResource(fileName)
    val parts = MultipartBodyBuilder().also {
      it.part("file", file)
    }.build()

    // upload file
    client.post().uri {
        it.path("/")
          .queryParam("module", randomModuleValue())
          .queryParam("name", "logback-test")
          .queryParam("type", "xml")
          .build()
      }
      .contentType(MULTIPART_FORM_DATA) // not explicit set this also ok
      //.contentLength(fileSize) // setting this will truncate the content
      .body(BodyInserters.fromMultipartData(parts))
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
    val parts = MultipartBodyBuilder().also {
      it.part("file", file)
    }.build()

    // upload file
    client.post().uri("/")
      .contentType(MULTIPART_FORM_DATA) // not explicit set this also ok
      //.contentLength(fileSize) // setting this will truncate the content
      .body(BodyInserters.fromMultipartData(parts))
      .exchange()
      .expectStatus().isCreated
      .expectHeader().contentTypeCompatibleWith(TEXT_PLAIN)
      .expectBody<String>().consumeWith {
        assertThat(it.responseBody).hasSizeGreaterThan(0)
      }
  }
}