package tech.simter.file.rest.webflux.handler

import io.mockk.every
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.core.io.ClassPathResource
import org.springframework.http.MediaType
import org.springframework.http.MediaType.APPLICATION_OCTET_STREAM
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import reactor.core.publisher.Mono
import tech.simter.exception.PermissionDeniedException
import tech.simter.file.core.FileService
import tech.simter.file.core.FileUpdateDescriber
import tech.simter.file.core.FileUploadSource
import tech.simter.file.rest.webflux.UnitTestConfiguration
import tech.simter.util.RandomUtils.randomString
import java.util.*

/**
 * Test update file by ajax.
 *
 * @author nb
 */
@SpringJUnitConfig(UnitTestConfiguration::class)
@WebFluxTest
class UpdateByStreamTest @Autowired constructor(
  @Value("\${simter-file.rest-context-path}")
  private val contextPath: String,
  private val client: WebTestClient,
  private val service: FileService
) {
  @Test
  fun success() {
    // prepare data
    val id = randomString()
    val module = randomString()
    val name = "logback-test"
    val type = "xml"
    val fileUpdateDescriber = FileUpdateDescriber.Impl(
      module = Optional.of(module),
      name = Optional.of(name),
      type = Optional.of(type)
    )
    val resource = ClassPathResource("$name.$type")

    // mock
    every { service.update(id, fileUpdateDescriber, any()) } returns Mono.empty()

    // invoke request
    client.patch().uri {
      it.path("$contextPath/${id}")
        .queryParam("module", "{module}")
        .queryParam("name", name)
        .queryParam("type", type)
        .build(module)
    }
      .contentType(APPLICATION_OCTET_STREAM)
      .contentLength(resource.contentLength())
      .bodyValue(resource.file.readBytes())
      .exchange()
      .expectStatus().isNoContent

    // verify
    verify {
      service.update(id, fileUpdateDescriber,
        match {
          assertThat(it.get()).isExactlyInstanceOf(FileUploadSource.FromDataBufferPublisher::class.java)
          true
        })
    }
  }

  @Test
  fun `failed by permission denied`() {
    // prepare data
    val id = randomString()
    val module = randomString()
    val name = "logback-test"
    val type = "xml"
    val fileUpdateDescriber = FileUpdateDescriber.Impl(
      module = Optional.of(module),
      name = Optional.of(name),
      type = Optional.of(type)
    )
    val resource = ClassPathResource("$name.$type")

    // mock
    val msg = randomString()
    every { service.update(id, fileUpdateDescriber, any()) } returns Mono.error(PermissionDeniedException(msg))

    // invoke request
    client.patch().uri {
      it.path("$contextPath/${id}")
        .queryParam("module", "{module}")
        .queryParam("name", name)
        .queryParam("type", type)
        .build(module)
    }
      .contentType(APPLICATION_OCTET_STREAM)
      .contentLength(resource.contentLength())
      .bodyValue(resource.file.readBytes())
      .exchange()
      .expectStatus().isForbidden
      .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_PLAIN)
      .expectBody<String>().isEqualTo(msg)

    // verify
    verify(exactly = 1) {
      service.update(id, fileUpdateDescriber,
        match {
          assertThat(it.get()).isExactlyInstanceOf(FileUploadSource.FromDataBufferPublisher::class.java)
          true
        })
    }
  }
}