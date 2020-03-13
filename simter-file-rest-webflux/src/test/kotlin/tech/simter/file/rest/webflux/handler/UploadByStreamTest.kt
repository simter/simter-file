package tech.simter.file.rest.webflux.handler

import io.mockk.every
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.core.io.ClassPathResource
import org.springframework.http.MediaType.APPLICATION_OCTET_STREAM
import org.springframework.http.MediaType.TEXT_PLAIN
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import reactor.core.publisher.Mono
import tech.simter.file.core.FileService
import tech.simter.file.core.FileUploadSource
import tech.simter.file.rest.webflux.UnitTestConfiguration
import tech.simter.file.test.TestHelper.randomFileStore

/**
 * Test upload file by ajax.
 *
 * @author RJ
 */
@SpringJUnitConfig(UnitTestConfiguration::class)
@WebFluxTest
class UploadByStreamTest @Autowired constructor(
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
      .contentType(APPLICATION_OCTET_STREAM)
      .contentLength(file.size)
      .bodyValue(resource.file.readBytes())
      .exchange()
      .expectStatus().isCreated
      .expectHeader().contentTypeCompatibleWith(TEXT_PLAIN)
      .expectBody<String>().consumeWith {
        assertThat(it.responseBody).isEqualTo(file.id)
      }

    // verify
    verify {
      service.upload(
        match {
          assertThat(it).isEqualToComparingFieldByField(file)
          true
        },
        match {
          assertThat(it).isExactlyInstanceOf(FileUploadSource.FromDataBufferPublisher::class.java)
          true
        })
    }
  }
}