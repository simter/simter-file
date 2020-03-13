package tech.simter.file.test.rest

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.test.web.reactive.server.WebTestClient
import tech.simter.reactive.web.Utils.createClientHttpConnector

@Configuration
class UnitTestConfiguration @Autowired constructor(
  @Value("\${simter-file.server-url}")
  private val serverUrl: String,
  @Value("\${proxy.host:#{null}}")
  private val proxyHost: String?,
  @Value("\${proxy.port:#{null}}")
  private val proxyPort: Int?
) {
  @Bean
  fun webTestClient(): WebTestClient {
    //return WebTestClient.bindToServer().baseUrl(serverUrl).build()
    return WebTestClient
      .bindToServer(
        createClientHttpConnector(
          proxyHost = proxyHost,
          proxyPort = proxyPort,
          connectTimeout = 5,
          readTimeout = 5,
          writeTimeout = 5
        )
      )
      .baseUrl(serverUrl)
      .build()
  }
}