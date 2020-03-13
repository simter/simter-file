package tech.simter.file.test.rest

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType.APPLICATION_XML
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import tech.simter.file.buildContentDisposition
import tech.simter.file.test.TestHelper.randomModuleValue
import tech.simter.file.test.rest.TestHelper.findAllFileView
import tech.simter.file.test.rest.TestHelper.uploadOneFile
import java.net.URLEncoder
import java.nio.file.Paths

/**
 * Test download file by file path.
 *
 * `GET /$id?type=path`
 *
 * @author RJ
 */
@SpringBootTest(classes = [UnitTestConfiguration::class])
class DownloadByPathTest @Autowired constructor(
  private val client: WebTestClient
) {
  @Test
  fun found() {
    // prepare data
    val module = randomModuleValue()
    uploadOneFile(client = client, module = module)
    val fileViews = findAllFileView(client = client, module = module)
    assertThat(fileViews).hasSize(1)
    val file = fileViews.first()
    val encodedPath = URLEncoder.encode(file.path, "UTF-8")
    val fileName = Paths.get(file.path).fileName.toString()

    // 1. download with default attachment mode
    client.get().uri("/$encodedPath?type=path")
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
    client.get().uri("/$encodedPath?type=path&inline")
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
    client.get().uri("/$encodedPath?type=path&filename=$customFileName")
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
    val path: String = URLEncoder.encode("/not-exists-dir/not-exists-file.xyz", "UTF-8")
    client.get().uri("/$path?type=path")
      .exchange()
      .expectStatus().isNotFound
  }
}