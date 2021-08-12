package tech.simter.file.rest.webflux.handler

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.http.MediaType.APPLICATION_XML
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import tech.simter.file.BASE_DATA_DIR
import tech.simter.file.buildContentDisposition
import tech.simter.file.rest.webflux.UnitTestConfiguration
import java.nio.file.Paths

/**
 * Test download file by file path.
 *
 * `GET /$id?type=path`
 *
 * @author RJ
 */
@SpringJUnitConfig(UnitTestConfiguration::class)
@WebFluxTest
@TestPropertySource(properties = ["$BASE_DATA_DIR=target"])
class DownloadByPathTest @Autowired constructor(
  @Value("\${simter-file.rest-context-path}")
  private val contextPath: String,
  @Value("\${$BASE_DATA_DIR}")
  private val baseDir: String,
  private val client: WebTestClient
) {
  @Test
  fun found() {
    // prepare data
    val path = "test-classes/logback-test.xml"
    val fileName = Paths.get(path).fileName.toString()
    val fileSize = Paths.get(baseDir, path).toFile().length()

    // 1. download with default attachment mode
    client.get().uri("$contextPath/{path}?type=path", path)
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(APPLICATION_XML)
      .expectHeader().contentLength(fileSize)
      .expectHeader().value("Content-Disposition") {
        assertThat(it).isEqualTo(buildContentDisposition("attachment", fileName))
      }
      .expectBody<String>().consumeWith {
        assertThat(it.responseBody).hasSize(fileSize.toInt())
      }

    // 2. download with inline mode
    client.get().uri("$contextPath/{path}?type=path&inline", path)
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(APPLICATION_XML)
      .expectHeader().contentLength(fileSize)
      .expectHeader().value("Content-Disposition") {
        assertThat(it).isEqualTo(buildContentDisposition("inline", fileName))
      }
      .expectBody<String>().consumeWith {
        assertThat(it.responseBody).hasSize(fileSize.toInt())
      }

    // 3. download with custom filename
    val customFileName = "abc-123-中文.xml"
    client.get().uri("$contextPath/{path}?type=path&filename=$customFileName", path)
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(APPLICATION_XML)
      .expectHeader().contentLength(fileSize)
      .expectHeader().value("Content-Disposition") {
        assertThat(it).isEqualTo(buildContentDisposition("attachment", customFileName))
      }
      .expectBody<String>().consumeWith {
        assertThat(it.responseBody).hasSize(fileSize.toInt())
      }
  }

  @Test
  fun notFound() {
    val path = "not-exists-dir/not-exists-file.xyz"
    client.get().uri("$contextPath/{path}?type=path", path)
      .exchange()
      .expectStatus().isNotFound
  }
}