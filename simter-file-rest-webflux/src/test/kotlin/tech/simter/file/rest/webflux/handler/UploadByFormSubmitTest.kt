package tech.simter.file.rest.webflux.handler

import io.mockk.every
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.core.io.ClassPathResource
import org.springframework.http.MediaType
import org.springframework.http.MediaType.MULTIPART_FORM_DATA
import org.springframework.http.client.MultipartBodyBuilder
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.web.reactive.function.BodyInserters
import reactor.core.publisher.Mono
import tech.simter.file.core.FileService
import tech.simter.file.core.FileUploadSource
import tech.simter.file.rest.webflux.UnitTestConfiguration
import tech.simter.file.test.TestHelper.randomFileStore

/**
 * Test upload file by traditional form submit.
 *
 * @author RJ
 */
@SpringJUnitConfig(UnitTestConfiguration::class)
@WebFluxTest
class UploadByFormSubmitTest @Autowired constructor(
  private val client: WebTestClient,
  private val service: FileService
) {
  @Test
  fun success() {
    // prepare data
    val name = "logback-test"
    val type = "xml"
    val resource = ClassPathResource("$name.$type")
    val file = randomFileStore(
      name = name,
      type = type,
      size = resource.contentLength()
    )
    val part = MultipartBodyBuilder().also {
      it.part("file", resource)
    }.build()

    // mock
    every { service.upload(any(), any()) } returns Mono.just(file.id)

    // invoke request
    client.post().uri {
        it.path("/")
          .queryParam("module", file.module)
          .queryParam("name", file.name)
          .queryParam("type", file.type)
          .build()
      }
      .contentType(MULTIPART_FORM_DATA) // not explicit set this also ok
      //.contentLength(fileSize) // setting this will truncate the content
      .body(BodyInserters.fromMultipartData(part))
      .exchange()
      .expectStatus().isCreated
      .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_PLAIN)
      .expectBody<String>().consumeWith {
        assertThat(it.responseBody).hasSizeGreaterThan(0)
      }

    // verify
    verify {
      service.upload(
        match {
          assertThat(it).isEqualToComparingFieldByField(file)
          true
        },
        match {
          assertThat(it).isExactlyInstanceOf(FileUploadSource.FromFilePart::class.java)
          true
        })
    }
  }
}