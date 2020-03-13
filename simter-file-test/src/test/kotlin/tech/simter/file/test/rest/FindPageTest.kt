package tech.simter.file.test.rest

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.test.web.reactive.server.WebTestClient
import tech.simter.file.test.TestHelper.randomModuleValue
import tech.simter.file.test.rest.TestHelper.uploadOneFile

/**
 * Test find files pageable.
 *
 * @author RJ
 */
@SpringBootTest(classes = [UnitTestConfiguration::class])
class FindPageTest @Autowired constructor(
  private val client: WebTestClient
) {
  @Test
  fun found() {
    // prepare data
    val module = randomModuleValue()
    val r0 = uploadOneFile(client = client, module = module)
    val r1 = uploadOneFile(client = client, module = module)

    // find it
    val limit = 20
    client.get().uri("/?pageable&module=$module&limit=$limit")
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(APPLICATION_JSON)
      .expectBody()
      .jsonPath("$.offset").isEqualTo(0)
      .jsonPath("$.limit").isEqualTo(limit)
      .jsonPath("$.total").isEqualTo(2)
      .jsonPath("$.rows.size()").isEqualTo(2)

      .jsonPath("$.rows[0].id").isEqualTo(r1.id)
      .jsonPath("$.rows[0].module").isEqualTo(r1.describer.module)
      .jsonPath("$.rows[0].name").isEqualTo(r1.describer.name)
      .jsonPath("$.rows[0].type").isEqualTo(r1.describer.type)
      .jsonPath("$.rows[0].size").isEqualTo(r1.describer.size)

      .jsonPath("$.rows[1].id").isEqualTo(r0.id)
      .jsonPath("$.rows[1].module").isEqualTo(r1.describer.module)
      .jsonPath("$.rows[1].name").isEqualTo(r1.describer.name)
      .jsonPath("$.rows[1].type").isEqualTo(r1.describer.type)
      .jsonPath("$.rows[1].size").isEqualTo(r1.describer.size)
  }

  @Test
  fun notFound() {
    val module = randomModuleValue()
    val limit = 20
    client.get().uri("/?pageable&module=$module&limit=$limit")
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.offset").isEqualTo(0)
      .jsonPath("$.limit").isEqualTo(limit)
      .jsonPath("$.total").isEqualTo(0)
      .jsonPath("$.rows.size()").isEqualTo(0)
  }
}