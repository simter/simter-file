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
 * Test find files none-pageable.
 *
 * @author RJ
 */
@SpringJUnitConfig(UnitTestConfiguration::class)
@WebFluxTest
@TestInstance(PER_CLASS)
class FindListTest @Autowired constructor(
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
    client.get().uri("$url?module={module}", module)
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

  @ParameterizedTest
  @MethodSource("urlProvider")
  fun notFound(url: String) {
    client.get().uri("$url?module={module}", randomModuleValue())
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .json("[]")
      .jsonPath("$.size()").isEqualTo(0)
  }
}