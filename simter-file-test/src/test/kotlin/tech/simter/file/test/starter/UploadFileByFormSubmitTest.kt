package tech.simter.file.test.starter

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.io.ClassPathResource
import org.springframework.http.MediaType.MULTIPART_FORM_DATA
import org.springframework.http.client.MultipartBodyBuilder
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.BodyInserters
import tech.simter.file.test.TestHelper.randomAttachmentId

/**
 * Test `POST /` through form submit.
 *
 * @author RJ
 */
@Disabled
@SpringBootTest(classes = [IntegrationTestConfiguration::class])
class UploadFileByFormSubmitTest @Autowired constructor(
  private val client: WebTestClient
) {
  @Test
  fun test() {
    // mock
    val fileName = "application.yml"
    val file = ClassPathResource(fileName)
    val upperId = randomAttachmentId()
    val fileSize = file.contentLength()
    val puid = "text"
    val parts = MultipartBodyBuilder().also {
      it.part("fileData", file)
      it.part("upperId", upperId)
      it.part("puid", puid)
    }.build()

    // upload file
    client.post().uri("/")
      .contentType(MULTIPART_FORM_DATA)
      .contentLength(fileSize)
      .body(BodyInserters.fromMultipartData(parts))
      .exchange()
      .expectStatus().isCreated
      .expectHeader().value("Location") {
        Assertions.assertThat(it).startsWith("/")
          .hasSizeGreaterThan(1)
        // TODO verify file created
        //println("Location=$it")
      }
  }
}