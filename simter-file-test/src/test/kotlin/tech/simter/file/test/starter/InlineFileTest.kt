package tech.simter.file.test.starter

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.ContentDisposition.parse
import org.springframework.http.MediaType.APPLICATION_XML
import org.springframework.test.web.reactive.server.WebTestClient
import tech.simter.file.test.TestHelper.randomAttachmentId
import tech.simter.file.test.starter.TestHelper.uploadOneFile

/**
 * Test `GET /inline/$id`.
 *
 * @author RJ
 */
@SpringBootTest(classes = [IntegrationTestConfiguration::class])
class InlineFileTest @Autowired constructor(
  private val client: WebTestClient
) {
  @Test
  fun found() {
    // upload one file
    val attachment = uploadOneFile(client)

    // invoke request
    val responseBody = client.get().uri("/inline/${attachment.id}")
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(APPLICATION_XML)
      .expectHeader().contentLength(attachment.size)
      .expectHeader().contentDisposition(parse("inline; filename=\"${attachment.fileName}\""))
      .expectBody().returnResult().responseBody

    // verify response body
    assertThat(responseBody).hasSize(attachment.size.toInt())
  }

  @Test
  fun notFound() {
    client.get().uri("/inline/${randomAttachmentId()}")
      .exchange()
      .expectStatus().isNotFound
  }
}