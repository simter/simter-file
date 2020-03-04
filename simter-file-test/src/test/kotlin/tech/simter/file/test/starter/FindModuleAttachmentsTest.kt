package tech.simter.file.test.starter

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.test.web.reactive.server.WebTestClient
import tech.simter.file.test.TestHelper.randomAttachmentId
import tech.simter.file.test.TestHelper.randomString
import tech.simter.file.test.starter.TestHelper.uploadOneFile

/**
 * Test `GET /parent/$puid/$upperId`.
 *
 * @author RJ
 */
@SpringBootTest(classes = [IntegrationTestConfiguration::class])
class FindModuleAttachmentsTest @Autowired constructor(
  private val client: WebTestClient
) {
  @Test
  fun found() {
    // upload one file
    val attachment = uploadOneFile(client)

    // invoke
    client.get().uri("/parent/${attachment.puid}/${attachment.upperId}")
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(APPLICATION_JSON)
      .expectBody()
      .jsonPath("$[0].id").isEqualTo(attachment.id)
      .jsonPath("$[0].puid").isEqualTo(attachment.puid!!)
      .jsonPath("$[0].upperId").isEqualTo(attachment.upperId!!)
      .jsonPath("$[0].name").isEqualTo(attachment.name)
      .jsonPath("$[0].type").isEqualTo(attachment.type)
      .jsonPath("$[0].size").isEqualTo(attachment.size)
  }

  @Test
  fun notFound() {
    val puid = randomString()
    val upperId = randomAttachmentId()
    client.get().uri("/parent/$puid/$upperId")
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .json("[]")
  }
}