package tech.simter.file.rest.webflux.handler

import io.mockk.every
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.core.io.ClassPathResource
import org.springframework.http.MediaType.APPLICATION_OCTET_STREAM
import org.springframework.http.MediaType.TEXT_PLAIN
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import reactor.core.publisher.Mono
import tech.simter.exception.PermissionDeniedException
import tech.simter.file.core.FileService
import tech.simter.file.core.FileUploadSource
import tech.simter.file.rest.webflux.UnitTestConfiguration
import tech.simter.file.test.TestHelper.randomFileStore
import tech.simter.util.RandomUtils.randomString
import java.util.stream.Stream

/**
 * Test upload file by ajax.
 *
 * @author RJ
 */
@SpringJUnitConfig(UnitTestConfiguration::class)
@WebFluxTest
@TestInstance(PER_CLASS)
class UploadByStreamTest @Autowired constructor(
  @Value("\${simter-file.rest-context-path}")
  private val contextPath: String,
  private val client: WebTestClient,
  private val service: FileService
) {
  private fun urlProvider(): Stream<String> {
    return Stream.of(contextPath, "$contextPath/")
  }

  @ParameterizedTest
  @MethodSource("urlProvider")
  fun success(url: String) {
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
      it.path(url)
        .queryParam("module", "{module}")
        .queryParam("name", file.name)
        .queryParam("type", file.type)
        .build(file.module)
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
          assertThat(it).usingRecursiveComparison().isEqualTo(file)
          true
        },
        match {
          assertThat(it).isExactlyInstanceOf(FileUploadSource.FromDataBufferPublisher::class.java)
          true
        })
    }
  }

  @ParameterizedTest
  @MethodSource("urlProvider")
  fun `failed by permission denied`(url: String) {
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
    val msg = randomString()
    every { service.upload(any(), any()) } returns Mono.error(PermissionDeniedException(msg))

    // invoke request
    client.post().uri {
      it.path(url)
        .queryParam("module", "{module}")
        .queryParam("name", file.name)
        .queryParam("type", file.type)
        .build(file.module)
    }
      .contentType(APPLICATION_OCTET_STREAM)
      .contentLength(file.size)
      .bodyValue(resource.file.readBytes())
      .exchange()
      .expectStatus().isForbidden
      .expectHeader().contentTypeCompatibleWith(TEXT_PLAIN)
      .expectBody<String>().isEqualTo(msg)

    // verify
    verify(exactly = 1) {
      service.upload(
        match {
          assertThat(it).usingRecursiveComparison().isEqualTo(file)
          true
        },
        match {
          assertThat(it).isExactlyInstanceOf(FileUploadSource.FromDataBufferPublisher::class.java)
          true
        })
    }
  }
}