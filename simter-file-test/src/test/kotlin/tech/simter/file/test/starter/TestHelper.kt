package tech.simter.file.test.starter

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonConfiguration
import org.assertj.core.api.Assertions.assertThat
import org.springframework.core.io.ClassPathResource
import org.springframework.http.MediaType.APPLICATION_OCTET_STREAM
import org.springframework.test.web.reactive.server.WebTestClient
import tech.simter.file.core.domain.Attachment
import tech.simter.file.impl.domain.AttachmentImpl
import java.time.OffsetDateTime

object TestHelper {
  /**
   * Kotlin 的 Json 扩展实例。
   */
  private val json = kotlinx.serialization.json.Json(JsonConfiguration.Stable.copy(strictMode = false))

  /** upload one file and return the attachment id */
  fun uploadOneFile(
    client: WebTestClient
  ): Attachment {
    // mock
    val fileName = "logback-test.xml"
    val file = ClassPathResource(fileName)
    val fileData = file.file.readBytes()
    val upperId = "EMPTY"
    val fileSize = file.contentLength()
    val puid = "text"


    // upload file
    var id = ""
    client.post().uri("/?puid=$puid&upper=$upperId&filename=$fileName")
      .contentType(APPLICATION_OCTET_STREAM)
      .contentLength(fileSize)
      .bodyValue(fileData)
      .exchange()
      .expectStatus().isCreated
      .expectHeader().value("Location") {
        //println("Location=$it")
        id = it.substring(1)
      }
    assertThat(id).isNotEmpty()

    // get uploaded info
    val body = client.get().uri("/attachment/$id")
      .exchange()
      .expectStatus().isOk
      .expectBody(String::class.java)
      .returnResult()
      .responseBody!!
    //println("body=$body")

    // return attachment info
    return AttachmentImpl(
      id = id,
      name = "logback-test",
      type = "xml",
      size = fileSize,
      puid = puid,
      upperId = upperId,
      path = json.parse(Response.serializer(), body).path,
      createOn = OffsetDateTime.now(),
      creator = "tester",
      modifyOn = OffsetDateTime.now(),
      modifier = "tester"
    )
  }

  @Serializable
  private data class Response(
    val id: String,
    val path: String
  )
}