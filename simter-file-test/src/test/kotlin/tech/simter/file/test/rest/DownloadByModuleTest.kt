package tech.simter.file.test.rest

import kotlinx.serialization.json.Json
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType.APPLICATION_OCTET_STREAM
import org.springframework.http.MediaType.APPLICATION_XML
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.util.FileCopyUtils
import tech.simter.file.BASE_DATA_DIR
import tech.simter.file.buildContentDisposition
import tech.simter.file.test.TestHelper.randomModuleValue
import tech.simter.file.test.rest.TestHelper.findAllFileView
import tech.simter.file.test.rest.TestHelper.uploadOneFile
import java.net.URLEncoder.encode
import java.nio.file.Paths

/**
 * Test download file by file module.
 *
 * `GET /$id?type=module`
 *
 * @author RJ
 */
@SpringBootTest(classes = [UnitTestConfiguration::class])
class DownloadByModuleTest @Autowired constructor(
  @Value("\${$BASE_DATA_DIR}") private val baseDir: String,
  private val json: Json,
  private val client: WebTestClient
) {
  @Test
  fun onlyOneFile() {
    // prepare data
    val module = randomModuleValue()
    uploadOneFile(client = client, module = module)
    val fileViews = findAllFileView(client = client, module = module, json = json)
    assertThat(fileViews).hasSize(1)
    val file = fileViews.first()

    // download with default attachment mode
    client.get().uri("/${encode(module, "UTF-8")}?type=module")
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(APPLICATION_XML)
      .expectHeader().contentLength(file.size)
      .expectHeader().value("Content-Disposition") {
        assertThat(it).isEqualTo(buildContentDisposition(type = "attachment", filename = file.fileName))
      }
      .expectBody<String>().consumeWith {
        assertThat(it.responseBody).hasSize(file.size.toInt())
      }
  }

  @Test
  fun twoFiles() {
    // prepare data
    val module = randomModuleValue()
    val fuzzyModule = "$module%"
    uploadOneFile(client = client, module = module + "a/", name = "file1")
    uploadOneFile(client = client, module = module + "b/", name = "file2")
    val fileViews = findAllFileView(client = client, module = fuzzyModule, json = json)
    assertThat(fileViews).hasSize(2)

    // 1. download with default file name "unknown.zip"
    var zipFileSize = 0
    client.get().uri("/${encode(fuzzyModule, "UTF-8")}?type=module")
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(APPLICATION_OCTET_STREAM)
      .expectHeader().value("Content-Length") { len ->
        zipFileSize = len.toInt()
        assertThat(zipFileSize).isGreaterThan(0)
      }
      .expectHeader().value("Content-Disposition") {
        assertThat(it).isEqualTo(buildContentDisposition(type = "attachment", filename = "unknown.zip"))
      }
      .expectBody().consumeWith {
        assertThat(it.responseBody).hasSize(zipFileSize)

        // save to file
        saveZipFle(it.responseBody!!, module, "unknown.zip")
      }

    // 2. download with custom file name and simple mapper
    zipFileSize = 0
    client.get()
      //.uri("/${encode(module, "UTF-8")}?type=module&filename=test&mapper=test")
      .uri {
        it.path("/${encode(fuzzyModule, "UTF-8")}")
          .queryParam("type", "module")
          .queryParam("filename", "test")
          .queryParam("mapper", "test")
          .build()
      }
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(APPLICATION_OCTET_STREAM)
      .expectHeader().value("Content-Length") { len ->
        zipFileSize = len.toInt()
        assertThat(zipFileSize).isGreaterThan(0)
      }
      .expectHeader().value("Content-Disposition") {
        assertThat(it).isEqualTo(buildContentDisposition(type = "attachment", filename = "test.zip"))
      }
      .expectBody().consumeWith {
        assertThat(it.responseBody).hasSize(zipFileSize)

        // save to file
        saveZipFle(it.responseBody!!, module, "test1.zip")
      }

    // 3. download with custom file name and complex mapper
    zipFileSize = 0
    client.get()
      //.uri("/${encode(module, "UTF-8")}?type=module&filename=test&mapper=test")
      .uri {
        it.path("/${encode(fuzzyModule, "UTF-8")}")
          .queryParam("type", "module")
          .queryParam("filename", "test")
          .queryParam("mapper", encode("{\"${module}a/\": \"test-a\"}", "UTF-8"))
          .build()
      }
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(APPLICATION_OCTET_STREAM)
      .expectHeader().value("Content-Length") { len ->
        zipFileSize = len.toInt()
        assertThat(zipFileSize).isGreaterThan(0)
      }
      .expectHeader().value("Content-Disposition") {
        assertThat(it).isEqualTo(buildContentDisposition(type = "attachment", filename = "test.zip"))
      }
      .expectBody().consumeWith {
        assertThat(it.responseBody).hasSize(zipFileSize)

        // save to file
        saveZipFle(it.responseBody!!, module, "test2.zip")
      }
  }

  private fun saveZipFle(data: ByteArray, module: String, fileName: String) {
    val file = Paths.get(baseDir, module, fileName).toFile()
    if (!file.parentFile.exists()) file.parentFile.mkdirs()
    FileCopyUtils.copy(data, file)
  }

  @Test
  fun notFound() {
    client.get().uri("/${encode("/not-exists-module/", "UTF-8")}?type=module")
      .exchange()
      .expectStatus().isNotFound
  }
}