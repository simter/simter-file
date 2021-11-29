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
import org.springframework.http.MediaType.APPLICATION_XML
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import reactor.core.publisher.Mono
import tech.simter.exception.PermissionDeniedException
import tech.simter.file.buildContentDisposition
import tech.simter.file.core.FileDownload
import tech.simter.file.core.FileService
import tech.simter.file.rest.webflux.UnitTestConfiguration
import tech.simter.file.test.TestHelper.randomFileId
import tech.simter.file.test.TestHelper.randomFileStore
import tech.simter.util.RandomUtils
import java.nio.file.Paths

/**
 * Test download file by file id.
 *
 * `GET /$id`
 *
 * @author RJ
 */
@SpringJUnitConfig(UnitTestConfiguration::class)
@WebFluxTest
class DownloadByIdTest @Autowired constructor(
  @Value("\${simter-file.rest-context-path}")
  private val contextPath: String,
  private val client: WebTestClient,
  private val service: FileService
) {
  @Test
  fun found() {
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
    every { service.download(file.id) } returns Mono.just(FileDownload.Impl(
      describer = file,
      source = FileDownload.Source.FromPath(value = Paths.get("src/test/resources/${file.fileName}"))
    ))

    // 1. download with default attachment mode
    client.get().uri("$contextPath/${file.id}")
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(APPLICATION_XML)
      .expectHeader().contentLength(file.size)
      .expectHeader().value("Content-Disposition") {
        assertThat(it).isEqualTo(buildContentDisposition("attachment", file.fileName))
      }
      .expectBody<String>().consumeWith {
        assertThat(it.responseBody).hasSize(file.size.toInt())
      }
    verify(exactly = 1) { service.download(file.id) }

    // 2. download with inline mode
    client.get().uri("$contextPath/${file.id}?inline")
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(APPLICATION_XML)
      .expectHeader().contentLength(file.size)
      .expectHeader().value("Content-Disposition") {
        assertThat(it).isEqualTo(buildContentDisposition("inline", file.fileName))
      }
      .expectBody<String>().consumeWith {
        assertThat(it.responseBody).hasSize(file.size.toInt())
      }
    verify(exactly = 2) { service.download(file.id) }

    // 3. download with custom filename
    val customFileName = "abc-123-中文.xml"
    client.get().uri("$contextPath/${file.id}?filename=$customFileName")
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(APPLICATION_XML)
      .expectHeader().contentLength(file.size)
      .expectHeader().value("Content-Disposition") {
        assertThat(it).isEqualTo(buildContentDisposition("attachment", customFileName))
      }
      .expectBody<String>().consumeWith {
        assertThat(it.responseBody).hasSize(file.size.toInt())
      }
    verify(exactly = 3) { service.download(file.id) }
  }

  @Test
  fun notFound() {
    // mock
    every { service.download(any()) } returns Mono.empty()

    // invoke
    client.get().uri("$contextPath/${randomFileId()}")
      .exchange()
      .expectStatus().isNotFound

    // verify
    verify(exactly = 1) { service.download(any()) }
  }

  @Test
  fun `failed by permission denied`() {
    // mock
    val msg = RandomUtils.randomString()
    every { service.download(any()) } returns Mono.error(PermissionDeniedException(msg))

    // invoke
    client.get().uri("$contextPath/${randomFileId()}")
      .exchange()
      .expectStatus().isForbidden
      .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_PLAIN)
      .expectBody<String>().isEqualTo(msg)

    // verify
    verify(exactly = 1) { service.download(any()) }
  }
}