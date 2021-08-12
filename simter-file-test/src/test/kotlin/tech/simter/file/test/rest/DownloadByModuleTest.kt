package tech.simter.file.test.rest

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.http.MediaType.APPLICATION_OCTET_STREAM
import org.springframework.http.MediaType.APPLICATION_XML
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.util.FileCopyUtils
import tech.simter.file.buildContentDisposition
import tech.simter.file.test.TestHelper.randomModuleValue
import java.nio.file.Paths

/**
 * Test download file by file module.
 *
 * `GET /file/$id?type=module`
 *
 * @author RJ
 */
@SpringJUnitConfig(UnitTestConfiguration::class)
@WebFluxTest
@TestInstance(PER_CLASS)
class DownloadByModuleTest @Autowired constructor(
  @Value("\${server.context-path}")
  private val contextPath: String,
  @Value("\${data.dir}") private val dataDir: String,
  private val client: WebTestClient,
  private val helper: TestHelper
) {
  @Test
  fun onlyOneFile() {
    // prepare data
    val module = randomModuleValue()
    helper.uploadOneFile(module = module)
    val fileViews = helper.findAllFileView(module = module)
    assertThat(fileViews).hasSize(1)
    val file = fileViews.first()

    // download with default attachment mode
    client.get().uri("$contextPath/{module}?type=module", module)
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
    helper.uploadOneFile(module = module + "a/", name = "file1")
    helper.uploadOneFile(module = module + "b/", name = "file2")
    val fileViews = helper.findAllFileView(module = fuzzyModule)
    assertThat(fileViews).hasSize(2)

    // 1. download with default file name "unknown.zip"
    var zipFileSize = 0
    client.get().uri("$contextPath/{fuzzyModule}?type=module", fuzzyModule)
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
      //.uri("/{fuzzyModule}?type=module&filename=test&mapper=test")
      .uri {
        it.path("$contextPath/{fuzzyModule}")
          .queryParam("type", "module")
          .queryParam("filename", "test")
          .queryParam("mapper", "test")
          .build(fuzzyModule)
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
    val mapper = "{\"${module}a/\": \"test-a\"}"
    client.get()
      //.uri("/{fuzzyModule}?type=module&filename=test&mapper=test")
      .uri {
        it.path("$contextPath/{fuzzyModule}")
          .queryParam("type", "module")
          .queryParam("filename", "test")
          .queryParam("mapper", "{mapper}")
          .build(fuzzyModule, mapper)
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
    val file = Paths.get(dataDir, module, fileName).toFile()
    if (!file.parentFile.exists()) file.parentFile.mkdirs()
    FileCopyUtils.copy(data, file)
  }

  @Test
  fun notFound() {
    client.get().uri("$contextPath/{module}?type=module", "/not-exists-module/")
      .exchange()
      .expectStatus().isNotFound
  }
}