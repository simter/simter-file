package tech.simter.file.starter.webflux

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType.TEXT_PLAIN
import org.springframework.web.reactive.config.CorsRegistry
import org.springframework.web.reactive.config.EnableWebFlux
import org.springframework.web.reactive.config.WebFluxConfigurer
import org.springframework.web.reactive.function.server.ServerResponse.ok
import org.springframework.web.reactive.function.server.router

/**
 * Application WebFlux Configuration.
 *
 * see [WebFlux config API](https://docs.spring.io/spring/docs/current/spring-framework-reference/web-reactive.html#webflux-config-enable)
 *
 * @author RJ
 */
@Configuration("tech.simter.kv.starter.WebFluxConfiguration")
@EnableWebFlux
class WebFluxConfiguration : WebFluxConfigurer {
  /**
   * CORS config.
   *
   * See [Enabling CORS](https://docs.spring.io/spring/docs/current/spring-framework-reference/web-reactive.html#webflux-cors)
   */
  override fun addCorsMappings(registry: CorsRegistry?) {
    // Enabling CORS for the whole application
    // By default all origins and GET, HEAD, and POST methods are allowed
    registry!!.addMapping("/**")
      .allowedOrigins("*")
      .allowedMethods("*")
      .allowedHeaders("Authorization", "Content-Type", "Content-Disposition")
      //.exposedHeaders("header1")
      .allowCredentials(false)
      .maxAge(1800) // seconds
  }

  /**
   * Other application routes.
   */
  @Bean
  fun testRoutes() = router {
    "/test".nest {
      GET("/", { ok().contentType(TEXT_PLAIN).syncBody("test") })
    }
  }
}