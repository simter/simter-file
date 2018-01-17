package tech.simter.file.rest.webflux

import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.config.CorsRegistry
import org.springframework.web.reactive.config.EnableWebFlux
import org.springframework.web.reactive.config.WebFluxConfigurer

/**
 * WebFlux configuration for simter file server.
 * <p>
 * see <a href="https://docs.spring.io/spring/docs/current/spring-framework-reference/web-reactive.html#webflux-config-enable">WebFlux config API</a>
 *
 * @author RJ
 */
@Configuration
@EnableWebFlux
class WebFluxConfiguration : WebFluxConfigurer {
  /**
   * See <a href="https://docs.spring.io/spring/docs/current/spring-framework-reference/web-reactive.html#webflux-cors-java-config">Enabling CORS</a>
   */
  override fun addCorsMappings(registry: CorsRegistry?) {
    // Enabling CORS for the whole application
    // By default all origins and GET, HEAD, and POST methods are allowed
    registry!!.addMapping("/**")
  }
}