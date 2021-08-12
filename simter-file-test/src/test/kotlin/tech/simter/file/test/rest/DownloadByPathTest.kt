package tech.simter.file.test.rest

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.http.MediaType.APPLICATION_XML
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import tech.simter.file.buildContentDisposition
import tech.simter.file.test.TestHelper.randomModuleValue
import java.nio.file.Paths

/**
 * Test download file by file path.
 *
 * `GET /file/$id?type=path`
 *
 * @author RJ
 */
@SpringJUnitConfig(UnitTestConfiguration::class)
@WebFluxTest
@TestInstance(PER_CLASS)
class DownloadByPathTest @Autowired constructor(
  @Value("\${server.context-path}")
  private val contextPath: String,
  private val client: WebTestClient,
  private val helper: TestHelper
) {
  @Test
  fun found() {
    // prepare data
    val module = randomModuleValue()
    helper.uploadOneFile(module = module)
    val fileViews = helper.findAllFileView(module = module)
    assertThat(fileViews).hasSize(1)
    val file = fileViews.first()
    val fileName = Paths.get(file.path).fileName.toString()

    // 1. download with default attachment mode
    client.get().uri("$contextPath/{path}?type=path", file.path)
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(APPLICATION_XML)
      .expectHeader().contentLength(file.size)
      .expectHeader().value("Content-Disposition") {
        assertThat(it).isEqualTo(buildContentDisposition("attachment", fileName))
      }
      .expectBody<String>().consumeWith {
        assertThat(it.responseBody).hasSize(file.size.toInt())
      }

    // 2. download with inline mode
    client.get().uri("$contextPath/{path}?type=path&inline", file.path)
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(APPLICATION_XML)
      .expectHeader().contentLength(file.size)
      .expectHeader().value("Content-Disposition") {
        assertThat(it).isEqualTo(buildContentDisposition("inline", fileName))
      }
      .expectBody<String>().consumeWith {
        assertThat(it.responseBody).hasSize(file.size.toInt())
      }

    // 3. download with custom filename
    val customFileName = "abc-123-中文.xml"
    client.get().uri("$contextPath/{path}?type=path&filename=$customFileName", file.path)
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
  }

  @Test
  fun notFound() {
    val path = "/not-exists-dir/not-exists-file.xyz"
    client.get().uri("$contextPath/{path}?type=path", path)
      .exchange()
      .expectStatus().isNotFound
  }
}