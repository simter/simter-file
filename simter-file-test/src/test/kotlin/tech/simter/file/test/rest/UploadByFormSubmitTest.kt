package tech.simter.file.test.rest

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.core.io.ClassPathResource
import org.springframework.http.MediaType.MULTIPART_FORM_DATA
import org.springframework.http.MediaType.TEXT_PLAIN
import org.springframework.http.client.MultipartBodyBuilder
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.web.reactive.function.BodyInserters
import tech.simter.file.test.TestHelper.randomModuleValue
import java.util.stream.Stream

/**
 * Test upload file by traditional form submit.
 *
 * @author RJ
 */
@SpringJUnitConfig(UnitTestConfiguration::class)
@WebFluxTest
@TestInstance(PER_CLASS)
class UploadByFormSubmitTest @Autowired constructor(
  @Value("\${server.context-path}")
  private val contextPath: String,
  private val client: WebTestClient
) {
  private fun urlProvider(): Stream<String> {
    return Stream.of(contextPath, "$contextPath/")
  }

  @ParameterizedTest
  @MethodSource("urlProvider")
  fun specifyParams(url: String) {
    // prepare data
    val fileName = "logback-test.xml"
    val file = ClassPathResource(fileName)
    val parts = MultipartBodyBuilder().also {
      it.part("file", file)
    }.build()

    // upload file
    client.post().uri {
      it.path(url)
        .queryParam("module", "{module}")
        .queryParam("name", "logback-test")
        .queryParam("type", "xml")
        .build(randomModuleValue())
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

  @ParameterizedTest
  @MethodSource("urlProvider")
  fun noParams(url: String) {
    // prepare data
    val fileName = "logback-test.xml"
    val file = ClassPathResource(fileName)
    val parts = MultipartBodyBuilder().also {
      it.part("file", file)
    }.build()

    // upload file
    client.post().uri(url)
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