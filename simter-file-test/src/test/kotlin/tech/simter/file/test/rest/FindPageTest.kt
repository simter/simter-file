package tech.simter.file.test.rest

import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import org.springframework.test.web.reactive.server.WebTestClient
import tech.simter.file.test.TestHelper.randomModuleValue
import java.util.stream.Stream

/**
 * Test find files pageable.
 *
 * @author RJ
 */
@SpringJUnitConfig(UnitTestConfiguration::class)
@WebFluxTest
@TestInstance(PER_CLASS)
class FindPageTest @Autowired constructor(
  @Value("\${server.context-path}")
  private val contextPath: String,
  private val client: WebTestClient,
  private val helper: TestHelper
) {
  private fun urlProvider(): Stream<String> {
    return Stream.of(contextPath, "$contextPath/")
  }

  @ParameterizedTest
  @MethodSource("urlProvider")
  fun found(url: String) {
    // prepare data
    val module = randomModuleValue()
    val r0 = helper.uploadOneFile(module = module)
    val r1 = helper.uploadOneFile(module = module)

    // find it
    val limit = 20
    client.get().uri("$url?pageable&module={module}&limit=$limit", module)
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

  @ParameterizedTest
  @MethodSource("urlProvider")
  fun notFound(url: String) {
    val module = randomModuleValue()
    val limit = 20
    client.get().uri("$url?pageable&module={module}&limit=$limit", module)
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.offset").isEqualTo(0)
      .jsonPath("$.limit").isEqualTo(limit)
      .jsonPath("$.total").isEqualTo(0)
      .jsonPath("$.rows.size()").isEqualTo(0)
  }
}