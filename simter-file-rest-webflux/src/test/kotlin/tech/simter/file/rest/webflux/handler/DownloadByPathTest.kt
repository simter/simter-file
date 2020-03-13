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
import tech.simter.file.FILE_ROOT_DIR_KEY
import tech.simter.file.buildContentDisposition
import tech.simter.file.rest.webflux.UnitTestConfiguration
import java.net.URLEncoder
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
@TestPropertySource(properties = ["$FILE_ROOT_DIR_KEY=target"])
class DownloadByPathTest @Autowired constructor(
  @Value("\${$FILE_ROOT_DIR_KEY}")
  private val baseDir: String,
  private val client: WebTestClient
) {
  @Test
  fun found() {
    // prepare data
    val path = "test-classes/logback-test.xml"
    val encodedPath = URLEncoder.encode(path, "UTF-8")
    val fileName = Paths.get(path).fileName.toString()
    val fileSize = Paths.get(baseDir, path).toFile().length()

    // 1. download with default attachment mode
    client.get().uri("/$encodedPath?type=path")
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
    client.get().uri("/$encodedPath?type=path&inline")
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
    client.get().uri("/$encodedPath?type=path&filename=$customFileName")
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
    val path: String = URLEncoder.encode("not-exists-dir/not-exists-file.xyz", "UTF-8")
    client.get().uri("/$path?type=path")
      .exchange()
      .expectStatus().isNotFound
  }
}