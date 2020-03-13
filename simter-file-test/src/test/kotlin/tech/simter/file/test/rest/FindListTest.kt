package tech.simter.file.test.rest

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.test.web.reactive.server.WebTestClient
import tech.simter.file.test.TestHelper.randomModuleValue
import tech.simter.file.test.rest.TestHelper.uploadOneFile

/**
 * Test find files none-pageable.
 *
 * @author RJ
 */
@SpringBootTest(classes = [UnitTestConfiguration::class])
class FindListTest @Autowired constructor(
  private val client: WebTestClient
) {
  @Test
  fun found() {
    // prepare data
    val module = randomModuleValue()
    val r0 = uploadOneFile(client = client, module = module)
    val r1 = uploadOneFile(client = client, module = module)

    // find it
    client.get().uri("/?module=$module")
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(APPLICATION_JSON)
      .expectBody()
      .jsonPath("$.size()").isEqualTo(2)

      .jsonPath("$[0].id").isEqualTo(r1.id)
      .jsonPath("$[0].module").isEqualTo(r1.describer.module)
      .jsonPath("$[0].name").isEqualTo(r1.describer.name)
      .jsonPath("$[0].type").isEqualTo(r1.describer.type)
      .jsonPath("$[0].size").isEqualTo(r1.describer.size)

      .jsonPath("$[1].id").isEqualTo(r0.id)
      .jsonPath("$[1].module").isEqualTo(r1.describer.module)
      .jsonPath("$[1].name").isEqualTo(r1.describer.name)
      .jsonPath("$[1].type").isEqualTo(r1.describer.type)
      .jsonPath("$[1].size").isEqualTo(r1.describer.size)
  }

  @Test
  fun notFound() {
    client.get().uri("/?module=${randomModuleValue()}")
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .json("[]")
      .jsonPath("$.size()").isEqualTo(0)
  }
}