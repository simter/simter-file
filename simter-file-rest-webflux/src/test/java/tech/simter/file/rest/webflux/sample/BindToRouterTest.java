package tech.simter.file.rest.webflux.sample;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.web.reactive.server.WebTestClient;
import tech.simter.file.rest.webflux.WebFluxConfiguration;
import tech.simter.file.rest.webflux.handler.SystemInfoHandler;

import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8;

/**
 * see <a href="https://docs.spring.io/spring/docs/current/spring-framework-reference/testing.html#webtestclient">WebTestClient</a>
 */
@SpringJUnitConfig(classes = {
  WebFluxConfiguration.class, SystemInfoHandler.class
})
class BindToRouterTest {
  @Autowired
  private SystemInfoHandler handler;
  private WebTestClient client;

  @BeforeEach
  void setUp() {
    client = WebTestClient.bindToRouterFunction(handler.router()).build();
  }

  @Test
  void test() {
    client.get().uri("/")
      .accept(APPLICATION_JSON_UTF8)
      .exchange()
      .expectStatus().isOk()
      .expectHeader().contentType(APPLICATION_JSON_UTF8)
      .expectBody()
      .jsonPath(".projectStartTime").isNotEmpty();
  }
}