package tech.simter.file.rest.webflux.router

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.*
import org.springframework.web.reactive.function.server.RequestPredicates.GET
import reactor.core.publisher.Mono
import tech.simter.file.rest.webflux.handler.SystemInfoHandler

/**
 * All routers.configuration
 *
 * @author RJ
 */
@Component
class RouterConfiguration @Autowired constructor(
  private val systemInfoHandler: SystemInfoHandler
) : RouterFunction<ServerResponse> {
  override fun route(request: ServerRequest): Mono<HandlerFunction<ServerResponse>> {
    return RouterFunctions.route(GET("/"), systemInfoHandler)   // /root
      .andRoute(GET("/system-info"), systemInfoHandler)         // /system-info
      .route(request)
  }
}