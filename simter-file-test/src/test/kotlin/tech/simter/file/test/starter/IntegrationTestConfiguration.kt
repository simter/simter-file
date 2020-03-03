package tech.simter.file.test.starter

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.test.web.reactive.server.WebTestClient

@Configuration
class IntegrationTestConfiguration @Autowired constructor(
  @Value("\${simter-file.server-url}")
  private val serverUrl: String
) {
  @Bean
  fun webTestClient(): WebTestClient {
    return WebTestClient.bindToServer().baseUrl(serverUrl).build()
  }
}