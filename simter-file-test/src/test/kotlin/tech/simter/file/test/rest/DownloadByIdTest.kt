package tech.simter.file.test.rest

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType.APPLICATION_XML
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import tech.simter.file.buildContentDisposition
import tech.simter.file.test.TestHelper.randomFileId
import tech.simter.file.test.TestHelper.randomModuleValue
import tech.simter.file.test.rest.TestHelper.uploadOneFile

/**
 * Test download file by file id.
 *
 * `GET /$id`
 *
 * @author RJ
 */
@SpringBootTest(classes = [UnitTestConfiguration::class])
class DownloadByIdTest @Autowired constructor(
  private val client: WebTestClient
) {
  @Test
  fun found() {
    // prepare data
    val module = randomModuleValue()
    val r = uploadOneFile(client = client, module = module)
    val fileName = r.describer.fileName

    // 1. download with default attachment mode
    client.get().uri("/${r.id}")
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(APPLICATION_XML)
      .expectHeader().contentLength(r.describer.size)
      .expectHeader().value("Content-Disposition") {
        assertThat(it).isEqualTo(buildContentDisposition("attachment", fileName))
      }
      .expectBody<String>().consumeWith {
        assertThat(it.responseBody).hasSize(r.describer.size.toInt())
      }

    // 2. download with inline mode
    client.get().uri("/${r.id}?inline")
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(APPLICATION_XML)
      .expectHeader().contentLength(r.describer.size)
      .expectHeader().value("Content-Disposition") {
        assertThat(it).isEqualTo(buildContentDisposition("inline", fileName))
      }
      .expectBody<String>().consumeWith {
        assertThat(it.responseBody).hasSize(r.describer.size.toInt())
      }

    // 3. download with custom filename
    val customFileName = "abc-123-中文.xml"
    client.get().uri("/${r.id}?filename=$customFileName")
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(APPLICATION_XML)
      .expectHeader().contentLength(r.describer.size)
      .expectHeader().value("Content-Disposition") {
        assertThat(it).isEqualTo(buildContentDisposition("attachment", customFileName))
      }
      .expectBody<String>().consumeWith {
        assertThat(it.responseBody).hasSize(r.describer.size.toInt())
      }
  }

  @Test
  fun notFound() {
    client.get().uri("/${randomFileId()}")
      .exchange()
      .expectStatus().isNotFound
  }
}