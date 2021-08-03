package tech.simter.file.rest.webflux.handler

import io.mockk.every
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.core.io.ClassPathResource
import org.springframework.http.MediaType.MULTIPART_FORM_DATA
import org.springframework.http.client.MultipartBodyBuilder
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.BodyInserters
import reactor.core.publisher.Mono
import tech.simter.file.core.FileService
import tech.simter.file.core.FileUpdateDescriber
import tech.simter.file.core.FileUploadSource
import tech.simter.file.rest.webflux.UnitTestConfiguration
import tech.simter.util.RandomUtils.randomString
import java.util.*

/**
 * Test update file by traditional form submit.
 *
 * @author nb
 */
@SpringJUnitConfig(UnitTestConfiguration::class)
@WebFluxTest
class UpdateByFormSubmitTest @Autowired constructor(
  private val client: WebTestClient,
  private val service: FileService
) {
  @Test
  fun success() {
    // prepare data
    val id = randomString()
    val name = "logback-test"
    val type = "xml"
    val resource = ClassPathResource("$name.$type")
    val part = MultipartBodyBuilder().also {
      it.part("file", resource)
    }.build()
    val fileUpdateDescriber = FileUpdateDescriber.Impl(
      name = Optional.of(name),
      type = Optional.of(type)
    )

    // mock
    every { service.update(id, fileUpdateDescriber, any()) } returns Mono.empty()

    // invoke request
    client.patch().uri {
      it.path("/${id}")
        .queryParam("name", name)
        .queryParam("type", type)
        .build()
    }
      .contentType(MULTIPART_FORM_DATA)
      .body(BodyInserters.fromMultipartData(part))
      .exchange()
      .expectStatus().isNoContent

    // verify
    verify {
      service.update(id, fileUpdateDescriber,
        match {
          assertThat(it.get()).isExactlyInstanceOf(FileUploadSource.FromFilePart::class.java)
          true
        })
    }
  }
}