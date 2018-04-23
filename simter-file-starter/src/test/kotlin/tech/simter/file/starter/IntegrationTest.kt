package tech.simter.file.starter

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.test.web.reactive.server.WebTestClient

/**
 * Run test in the real server.
 * @author RJ
 */
@Disabled
class IntegrationTest {
  private val webClient = WebTestClient.bindToServer().baseUrl("http://localhost:9013").build()
  private val contextPath = ""

  @Test
  fun uploadOne() {
    // TODO
  }
}