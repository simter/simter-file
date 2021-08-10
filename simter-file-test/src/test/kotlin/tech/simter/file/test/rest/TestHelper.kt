package tech.simter.file.test.rest

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.assertj.core.api.Assertions.assertThat
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.ClassPathResource
import org.springframework.http.MediaType.*
import org.springframework.stereotype.Component
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import tech.simter.file.core.FileDescriber
import tech.simter.file.core.FileStore
import tech.simter.file.test.TestHelper.randomModuleValue

@Component
class TestHelper @Autowired constructor(
  @Value("\${server.context-path}")
  private val contextPath: String,
  private val json: Json,
  private val client: WebTestClient
) {
  data class Result(val id: String, val describer: FileDescriber)

  /** upload one file and return the file id */
  fun uploadOneFile(
    module: String = randomModuleValue(),
    name: String = "logback-test"
  ): Result {
    val originalName = "logback-test"
    val type = "xml"
    val file = ClassPathResource("$originalName.$type")
    val fileData = file.file.readBytes()
    val size = file.contentLength()

    // upload file
    var id = ""
    client.post().uri {
      it.path(contextPath)
        .queryParam("module", module)
        .queryParam("name", name)
        .queryParam("type", type)
        .build()
    }
      .contentType(APPLICATION_OCTET_STREAM)
      .contentLength(size)
      .bodyValue(fileData)
      .exchange()
      .expectStatus().isCreated
      .expectHeader().contentTypeCompatibleWith(TEXT_PLAIN)
      .expectBody<String>().consumeWith { id = it.responseBody!! }

    assertThat(id).isNotEmpty()

    // return file info
    return Result(
      id = id,
      describer = FileDescriber.Impl(
        module = module,
        name = name,
        type = type,
        size = size
      )
    )
  }

  fun findAllFileView(module: String): List<FileStore> {
    val body: String = client.get().uri("$contextPath?module=$module")
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(APPLICATION_JSON)
      .expectBody<String>()
      .returnResult().responseBody!!

    return json.decodeFromString<List<FileStore.Impl>>(body)
  }
}